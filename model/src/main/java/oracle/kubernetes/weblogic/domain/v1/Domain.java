// Copyright 2017, 2018, Oracle Corporation and/or its affiliates.  All rights reserved.
// Licensed under the Universal Permissive License v 1.0 as shown at
// http://oss.oracle.com/licenses/upl.

package oracle.kubernetes.weblogic.domain.v1;

import static oracle.kubernetes.operator.StartupControlConstants.AUTO_STARTUPCONTROL;

import com.google.gson.annotations.Expose;
import com.google.gson.annotations.SerializedName;
import io.kubernetes.client.models.V1ObjectMeta;
import io.kubernetes.client.models.V1SecretReference;
import java.util.Optional;
import javax.validation.Valid;
import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;
import org.apache.commons.lang3.builder.ToStringBuilder;

/** Domain represents a WebLogic domain and how it will be realized in the Kubernetes cluster. */
@SuppressWarnings("deprecation")
public class Domain {

  /**
   * APIVersion defines the versioned schema of this representation of an object. Servers should
   * convert recognized schemas to the latest internal value, and may reject unrecognized values.
   * More info: https://git.k8s.io/community/contributors/devel/api-conventions.md#resources
   */
  @SerializedName("apiVersion")
  @Expose
  private String apiVersion;
  /**
   * Kind is a string value representing the REST resource this object represents. Servers may infer
   * this from the endpoint the client submits requests to. Cannot be updated. In CamelCase. More
   * info: https://git.k8s.io/community/contributors/devel/api-conventions.md#types-kinds
   */
  @SerializedName("kind")
  @Expose
  private String kind;
  /**
   * Standard object's metadata. More info:
   * https://git.k8s.io/community/contributors/devel/api-conventions.md#metadata
   */
  @SerializedName("metadata")
  @Expose
  @Valid
  private V1ObjectMeta metadata;
  /** DomainSpec is a description of a domain. */
  @SerializedName("spec")
  @Expose
  @Valid
  private DomainSpec spec;
  /**
   * DomainStatus represents information about the status of a domain. Status may trail the actual
   * state of a system.
   */
  @SerializedName("status")
  @Expose
  @Valid
  private DomainStatus status;

  /**
   * APIVersion defines the versioned schema of this representation of an object. Servers should
   * convert recognized schemas to the latest internal value, and may reject unrecognized values.
   * More info: https://git.k8s.io/community/contributors/devel/api-conventions.md#resources
   *
   * @return API version
   */
  public String getApiVersion() {
    return apiVersion;
  }

  /**
   * APIVersion defines the versioned schema of this representation of an object. Servers should
   * convert recognized schemas to the latest internal value, and may reject unrecognized values.
   * More info: https://git.k8s.io/community/contributors/devel/api-conventions.md#resources
   *
   * @param apiVersion API version
   */
  public void setApiVersion(String apiVersion) {
    this.apiVersion = apiVersion;
  }

  /**
   * APIVersion defines the versioned schema of this representation of an object. Servers should
   * convert recognized schemas to the latest internal value, and may reject unrecognized values.
   * More info: https://git.k8s.io/community/contributors/devel/api-conventions.md#resources
   *
   * @param apiVersion API version
   * @return this
   */
  public Domain withApiVersion(String apiVersion) {
    this.apiVersion = apiVersion;
    return this;
  }

  /**
   * Kind is a string value representing the REST resource this object represents. Servers may infer
   * this from the endpoint the client submits requests to. Cannot be updated. In CamelCase. More
   * info: https://git.k8s.io/community/contributors/devel/api-conventions.md#types-kinds
   *
   * @return kind
   */
  public String getKind() {
    return kind;
  }

  /**
   * Kind is a string value representing the REST resource this object represents. Servers may infer
   * this from the endpoint the client submits requests to. Cannot be updated. In CamelCase. More
   * info: https://git.k8s.io/community/contributors/devel/api-conventions.md#types-kinds
   *
   * @param kind Kind
   */
  public void setKind(String kind) {
    this.kind = kind;
  }

  /**
   * Kind is a string value representing the REST resource this object represents. Servers may infer
   * this from the endpoint the client submits requests to. Cannot be updated. In CamelCase. More
   * info: https://git.k8s.io/community/contributors/devel/api-conventions.md#types-kinds
   *
   * @param kind Kind
   * @return this
   */
  public Domain withKind(String kind) {
    this.kind = kind;
    return this;
  }

  /**
   * Standard object's metadata. More info:
   * https://git.k8s.io/community/contributors/devel/api-conventions.md#metadata
   *
   * @return Metadata
   */
  public V1ObjectMeta getMetadata() {
    return metadata;
  }

  /**
   * Standard object's metadata. More info:
   * https://git.k8s.io/community/contributors/devel/api-conventions.md#metadata
   *
   * @param metadata Metadata
   */
  public void setMetadata(V1ObjectMeta metadata) {
    this.metadata = metadata;
  }

  /**
   * Standard object's metadata. More info:
   * https://git.k8s.io/community/contributors/devel/api-conventions.md#metadata
   *
   * @param metadata Metadata
   * @return this
   */
  public Domain withMetadata(V1ObjectMeta metadata) {
    this.metadata = metadata;
    return this;
  }

  public ServerSpec getAdminServerSpec() {
    return new ServerSpecV1Impl(spec).withServerStartup(getServerStartup(spec.getAsName()));
  }

  private ServerStartup getServerStartup(String name) {
    if (spec.getServerStartup() == null) return null;

    for (ServerStartup ss : spec.getServerStartup()) {
      if (ss.getServerName().equals(name)) {
        return ss;
      }
    }

    return null;
  }

  public ServerSpec getServer(String clusterName, String serverName) {
    return new ServerSpecV1Impl(spec)
        .withServerStartup(getServerStartup(serverName))
        .withClusterStartup(getClusterStartup(clusterName));
  }

  private ClusterStartup getClusterStartup(String clusterName) {
    if (spec.getClusterStartup() == null) return null;

    for (ClusterStartup cs : spec.getClusterStartup()) {
      if (cs.getClusterName().equals(clusterName)) {
        return cs;
      }
    }

    return null;
  }

  public int getReplicaLimit(String clusterName) {
    ClusterStartup clusterStartup = getClusterStartup(clusterName);
    return clusterStartup == null ? spec.getReplicas() : clusterStartup.getReplicas();
  }

  public String getStartupControl() {
    return Optional.ofNullable(spec.getStartupControl()).orElse(AUTO_STARTUPCONTROL).toUpperCase();
  }

  /**
   * DomainSpec is a description of a domain.
   *
   * @return Specification
   */
  public DomainSpec getSpec() {
    return spec;
  }

  /**
   * DomainSpec is a description of a domain.
   *
   * @param spec Specification
   */
  public void setSpec(DomainSpec spec) {
    this.spec = spec;
  }

  /**
   * DomainSpec is a description of a domain.
   *
   * @param spec Specification
   * @return this
   */
  public Domain withSpec(DomainSpec spec) {
    this.spec = spec;
    return this;
  }

  /**
   * DomainStatus represents information about the status of a domain. Status may trail the actual
   * state of a system.
   *
   * @return Status
   */
  public DomainStatus getStatus() {
    return status;
  }

  /**
   * DomainStatus represents information about the status of a domain. Status may trail the actual
   * state of a system.
   *
   * @param status Status
   */
  public void setStatus(DomainStatus status) {
    this.status = status;
  }

  /**
   * DomainStatus represents information about the status of a domain. Status may trail the actual
   * state of a system.
   *
   * @param status Status
   * @return this
   */
  public Domain withStatus(DomainStatus status) {
    this.status = status;
    return this;
  }

  /**
   * Reference to secret containing domain administrator username and password.
   *
   * @return admin secret
   */
  public V1SecretReference getAdminSecret() {
    return spec.getAdminSecret();
  }

  /**
   * Returns the name of the admin server.
   *
   * @return admin server name
   */
  public String getAsName() {
    return spec.getAsName();
  }

  /**
   * Returns the port used by the admin server.
   *
   * @return admin server port
   */
  public Integer getAsPort() {
    return spec.getAsPort();
  }
  /**
   * Returns the domain unique identifier.
   *
   * @return domain UID
   */
  public String getDomainUID() {
    return spec.getDomainUID();
  }

  /**
   * Returns the domain name
   *
   * @return domain name
   */
  public String getDomainName() {
    return spec.getDomainName();
  }

  @Override
  public String toString() {
    return new ToStringBuilder(this)
        .append("apiVersion", apiVersion)
        .append("kind", kind)
        .append("metadata", metadata)
        .append("spec", spec)
        .append("status", status)
        .toString();
  }

  @Override
  public int hashCode() {
    return new HashCodeBuilder()
        .append(metadata)
        .append(apiVersion)
        .append(kind)
        .append(spec)
        .append(status)
        .toHashCode();
  }

  @Override
  public boolean equals(Object other) {
    if (other == this) {
      return true;
    }
    if (!(other instanceof Domain)) {
      return false;
    }
    Domain rhs = ((Domain) other);
    return new EqualsBuilder()
        .append(metadata, rhs.metadata)
        .append(apiVersion, rhs.apiVersion)
        .append(kind, rhs.kind)
        .append(spec, rhs.spec)
        .append(status, rhs.status)
        .isEquals();
  }
}
