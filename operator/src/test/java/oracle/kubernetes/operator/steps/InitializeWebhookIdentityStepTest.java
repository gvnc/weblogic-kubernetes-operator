// Copyright (c) 2022, Oracle and/or its affiliates.
// Licensed under the Universal Permissive License v 1.0 as shown at https://oss.oracle.com/licenses/upl.

package oracle.kubernetes.operator.steps;

import java.net.URI;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

import com.meterware.simplestub.Memento;
import com.meterware.simplestub.StaticStubSupport;
import io.kubernetes.client.openapi.models.V1ObjectMeta;
import io.kubernetes.client.openapi.models.V1Secret;
import oracle.kubernetes.operator.ConversionWebhookMain;
import oracle.kubernetes.operator.ConversionWebhookMainTest;
import oracle.kubernetes.operator.helpers.KubernetesTestSupport;
import oracle.kubernetes.operator.helpers.TuningParametersStub;
import oracle.kubernetes.operator.utils.Certificates;
import oracle.kubernetes.operator.utils.InMemoryCertificates;
import oracle.kubernetes.operator.utils.InMemoryFileSystem;
import oracle.kubernetes.operator.work.Step;
import oracle.kubernetes.utils.SystemClockTestSupport;
import oracle.kubernetes.utils.TestUtils;
import org.hamcrest.Matchers;
import org.hamcrest.junit.MatcherAssert;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static com.meterware.simplestub.Stub.createStrictStub;
import static oracle.kubernetes.operator.EventConstants.CONVERSION_WEBHOOK_FAILED_EVENT;
import static oracle.kubernetes.operator.EventTestUtils.containsEventsWithCountOne;
import static oracle.kubernetes.operator.EventTestUtils.getEvents;
import static oracle.kubernetes.operator.helpers.NamespaceHelper.DEFAULT_NAMESPACE;
import static oracle.kubernetes.operator.steps.InitializeWebhookIdentityStep.WEBHOOK_KEY;
import static oracle.kubernetes.operator.steps.InitializeWebhookIdentityStep.WEBHOOK_SECRETS;
import static oracle.kubernetes.operator.utils.SelfSignedCertUtils.WEBHOOK_CERTIFICATE;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;

class InitializeWebhookIdentityStepTest {
  private final List<Memento> mementos = new ArrayList<>();
  private final KubernetesTestSupport testSupport = new KubernetesTestSupport();

  private final ConversionWebhookMainTest.ConversionWebhookMainDelegateStub delegate =
      createStrictStub(ConversionWebhookMainTest.ConversionWebhookMainDelegateStub.class, testSupport);
  private final Step initializeWebhookIdentityStep = new InitializeWebhookIdentityStep(delegate,
      new ConversionWebhookMain.CheckFailureAndCreateEventStep());

  public static final String NS = "namespace";
  private static InMemoryFileSystem inMemoryFileSystem = InMemoryFileSystem.createInstance();
  @SuppressWarnings({"FieldMayBeFinal", "CanBeFinal"})
  private static Function<String, Path> getInMemoryPath = p -> inMemoryFileSystem.getPath(p);
  @SuppressWarnings({"FieldMayBeFinal", "CanBeFinal"})
  private static Function<URI, Path> pathFunction = inMemoryFileSystem::getPath;

  @BeforeEach
  void setup() throws NoSuchFieldException {
    mementos.add(TestUtils.silenceOperatorLogger());

    mementos.add(testSupport.install());

    mementos.add(StaticStubSupport.install(InitializeWebhookIdentityStep.class, "getPath", getInMemoryPath));
    mementos.add(StaticStubSupport.install(InitializeWebhookIdentityStep.class, "uriToPath", pathFunction));
    mementos.add(StaticStubSupport.install(Certificates.class, "getPath", getInMemoryPath));

    mementos.add(SystemClockTestSupport.installClock());
    mementos.add(TuningParametersStub.install());
    mementos.add(InMemoryCertificates.install());
  }

  @AfterEach
  void tearDown() {
    mementos.forEach(Memento::revert);
  }

  @Test
  void whenNoWebhookIdentity_verifyIdentityIsInitialized() {
    Certificates certificates = new Certificates(delegate);

    testSupport.runSteps(initializeWebhookIdentityStep);

    MatcherAssert.assertThat(certificates.getWebhookCertificateData(), Matchers.notNullValue());
    MatcherAssert.assertThat(certificates.getWebhookKeyFilePath(),
        equalTo("/deployment/webhook-identity/webhookKey"));
  }

  @Test
  void whenWebhookIdentityExistsInFileSystemAndWritePermissionMissing_verifyFailedEventGenerated() {
    inMemoryFileSystem.defineFile("/deployment/secrets/webhookCert", "xyz");
    inMemoryFileSystem.defineFile("/deployment/secrets/webhookKey", "xyz");

    testSupport.runSteps(initializeWebhookIdentityStep);
    MatcherAssert.assertThat("Found 1 CONVERSION_FAILED_EVENT event with expected count 1",
        containsEventsWithCountOne(getEvents(testSupport),
            CONVERSION_WEBHOOK_FAILED_EVENT, 1), is(true));
  }

  @Test
  void whenWebhookIdentityExistsInConfigMapAndWritePermissionMissing_verifyFailedEventGenerated() {
    Map<String, byte[]> data = new HashMap<>();
    data.put(WEBHOOK_KEY, "xyz".getBytes());
    data.put(WEBHOOK_CERTIFICATE, "xyz".getBytes());
    V1Secret secret = new V1Secret().metadata(createSecretMetadata()).data(data);
    testSupport.defineResources(secret);
    testSupport.failOnResource(KubernetesTestSupport.SECRET, WEBHOOK_SECRETS, "TEST", 409);

    testSupport.runSteps(initializeWebhookIdentityStep);

    MatcherAssert.assertThat("Found 1 CONVERSION_FAILED_EVENT event with expected count 1",
        containsEventsWithCountOne(getEvents(testSupport),
            CONVERSION_WEBHOOK_FAILED_EVENT, 1), is(true));
  }

  @Test
  void whenNoWebhookIdentityAndReplaceFailsWithConflict_verifyIdentityIsInitialized() {
    Map<String, byte[]> data = new HashMap<>();
    V1Secret secret = new V1Secret().metadata(createSecretMetadata()).data(data);
    testSupport.defineResources(secret);
    testSupport.failOnReplace(KubernetesTestSupport.SECRET, WEBHOOK_SECRETS, DEFAULT_NAMESPACE, 409);

    Certificates certificates = new Certificates(delegate);

    testSupport.runSteps(initializeWebhookIdentityStep);

    MatcherAssert.assertThat(certificates.getWebhookCertificateData(), Matchers.notNullValue());
    MatcherAssert.assertThat(certificates.getWebhookKeyFilePath(),
        equalTo("/deployment/webhook-identity/webhookKey"));
  }

  @Test
  void whenWebhookIdentityExistsInConfigMapAndConflictError_verifyFailedEventGenerated() {
    Map<String, byte[]> data = new HashMap<>();
    data.put(WEBHOOK_KEY, "xyz".getBytes());
    data.put(WEBHOOK_CERTIFICATE, "xyz".getBytes());
    V1Secret secret = new V1Secret().metadata(createSecretMetadata()).data(data);
    testSupport.defineResources(secret);
    testSupport.failOnReplace(KubernetesTestSupport.SECRET, WEBHOOK_SECRETS, DEFAULT_NAMESPACE, 409);
    inMemoryFileSystem.defineFile("/deployment/secrets/webhookCert", "xyz");
    inMemoryFileSystem.defineFile("/deployment/secrets/webhookKey", "xyz");

    testSupport.runSteps(initializeWebhookIdentityStep);
    MatcherAssert.assertThat("Found 1 CONVERSION_FAILED_EVENT event with expected count 1",
        containsEventsWithCountOne(getEvents(testSupport),
            CONVERSION_WEBHOOK_FAILED_EVENT, 1), is(true));
  }

  @Test
  void whenNoWebhookIdentityAndReplaceFailsWith500Error_verifyIdentityIsInitialized() {
    Map<String, byte[]> data = new HashMap<>();
    V1Secret secret = new V1Secret().metadata(createSecretMetadata()).data(data);
    testSupport.defineResources(secret);
    testSupport.failOnReplace(KubernetesTestSupport.SECRET, WEBHOOK_SECRETS, DEFAULT_NAMESPACE, 500);

    Certificates certificates = new Certificates(delegate);

    testSupport.runSteps(initializeWebhookIdentityStep);

    MatcherAssert.assertThat(certificates.getWebhookCertificateData(), Matchers.notNullValue());
    MatcherAssert.assertThat(certificates.getWebhookKeyFilePath(),
        equalTo("/deployment/webhook-identity/webhookKey"));
  }

  private V1ObjectMeta createSecretMetadata() {
    return new V1ObjectMeta().name(WEBHOOK_SECRETS).namespace(DEFAULT_NAMESPACE);
  }
}