/**
 * Labels of nodes used in graph-db.
 * @see RelationType
 */
public enum NodeLabel {
        /**
         * {@link #Artifact} describes an maven artifact.
         * With properties:
         * - `groupId`
         * - `artifactId`
         * - `type`
         */
        Artifact,
        /**
         * {@link #ArtifactVersion} describes a version of an maven artifact.
         * With properties:
         * - `version`
         */
        ArtifactVersion,
        /**
         * {@link #ArtifactGroup} describes a group of maven artifacts.
         * With properties:
         * - `groupId`
         */
        ArtifactGroup,
        /**
         * {@link #JavaClass} describes a class provided by an maven artifact version.
         * With properties:
         * - `name`
         */
        JavaClass
}
