
public enum  RelationType {

    /**
     * Describe relation between a version ({@link NodeLabel#ArtifactVersion}) of an artifact ({@link NodeLabel#Artifact}).
     * <pre>
     *     (:ArtifactVersion) -[:IS_VERSION_OF]-> (:Artifact)
     * </pre>
     */
    IS_VERSION_OF,
    /**
     * Describe relation between a version ({@link NodeLabel#ArtifactVersion}) of an artifact ({@link NodeLabel#Artifact}).
     * <pre>
     *     (:Artifact) -[:IS_GROUP_OF]-> (:ArtifactGroup)
     *     (:ArtifactVersion) -[:IS_GROUP_OF]-> (:ArtifactGroup)
     * </pre>
     */
    IS_GROUP_OF,
    /**
     * Describe relation between a dependency of two versions of artifacts ({@link NodeLabel#ArtifactVersion}).
     * <pre>
     *     (:ArtifactVersion) -[:DEPEND_ON_ARTIFACT_VERSION]-> (:ArtifactVersion)
     * </pre>
     */
    DEPEND_ON_ARTIFACT_VERSION,
    /**
     * Describe relation between provided class ({@link NodeLabel#JavaClass}) of a artifact version ({@link NodeLabel#ArtifactVersion}).
     * <pre>
     *     (:ArtifactVersion) -[:PROVIDE_JAVA_CLASS]-> (:JavaClass)
     * </pre>
     */
    PROVIDE_JAVA_CLASS,
    /**
     * Describe relation between  class ({@link NodeLabel#JavaClass}) of a artifact version ({@link NodeLabel#ArtifactVersion}).
     * <pre>
     *     (:ArtifactVersion) -[:DEPEND_ON_EXTERNAL_JAVA_CLASS]-> (:JavaClass)
     * </pre>
     */
    DEPEND_ON_EXTERNAL_JAVA_CLASS,

    /**
     * Describe parent child relation between two version of artifacts ({@link NodeLabel#ArtifactVersion}).
     * This is ordinary a child artifact to parent POM relation.
     * <pre>
     *     (:ArtifactVersion) -[:IS_CHILD_OF_ARTIFACT_VERSION]-> (:ArtifactVersion)
     * </pre>
     */
    IS_CHILD_OF_ARTIFACT_VERSION
}
