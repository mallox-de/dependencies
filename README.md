# https://github.com/neo4j-contrib/neo4j-apoc-procedures/releases/tag/3.5.0.4


MATCH (a:Artifact {type:"jar",artifactId:"org.eclipse.jetty.websocket",groupId:"websocket-api"})
RETURN a

MATCH (n)-->(m)
RETURN n,m

MATCH (a:Artifact)-[r]-()
WITH a, count(r) as sstcount
WHERE sstcount > 1
MATCH (a)<--(n)
RETURN a, n

MATCH (a:Artifact {  type: "jar",  artifactId: "net.sourceforge.htmlunit",  groupId: "htmlunit" })
RETURN a

MATCH (a:Artifact {  type: "jar",  artifactId: "net.sourceforge.htmlunit",  groupId: "htmlunit" })
MATCH (a)-[*..5]-(b:Artifact)
RETURN a,b

MATCH (a:Artifact {  type: "jar",  artifactId: "net.sourceforge.htmlunit",  groupId: "htmlunit" })
MATCH (a)-[*..5]-()-[r:IS_VERSION_OF]->(b:Artifact)
WITH b, count(r) as sstcount
WHERE sstcount > 1
RETURN b

# Conflicts

# Variante a:
MATCH (a:Artifact {  type: "jar",  groupId: "net.sourceforge.htmlunit",  artifactId: "htmlunit" })<--(av:ArtifactVersion {version:'2.35.0'})
MATCH (av)-[:DEPEND_ON_ARTIFACT_VERSION*]->(oav1:ArtifactVersion)
MATCH (av)-[:DEPEND_ON_ARTIFACT_VERSION*]->(oav2:ArtifactVersion)
WITH oav1,oav2
MATCH (oav1)-[:IS_VERSION_OF]->(conflict:Artifact)<-[:IS_VERSION_OF]-(oav2)
WHERE oav1 <> oav2
RETURN oav1,oav2,conflict

# Variante b:
MATCH (a:Artifact {  type: "jar",  groupId: "net.sourceforge.htmlunit",  artifactId: "htmlunit" })<--(av:ArtifactVersion {version:'2.35.0'})
MATCH (av)-[:DEPEND_ON_ARTIFACT_VERSION*]->(oav1:ArtifactVersion)
MATCH (av)-[:DEPEND_ON_ARTIFACT_VERSION*]->(oav2:ArtifactVersion)
WITH a,av,oav1,oav2
MATCH (oav1)-[:IS_VERSION_OF]->(conflict:Artifact)<-[:IS_VERSION_OF]-(oav2)
WHERE oav1 <> oav2
RETURN a,av,oav1,oav2,conflict

# Variante b optimiert:

https://neo4j.com/developer/kb/all-shortest-paths-between-set-of-nodes/

https://community.neo4j.com/t/all-shortest-paths-between-a-set-of-nodes/241/5
https://neo4j-contrib.github.io/neo4j-apoc-procedures/#_expand_paths

https://stackoverflow.com/questions/36897634/there-is-no-procedure-with-the-name-apoc-help-registered-for-this-database-ins


MATCH (a:Artifact {  type: "jar",  groupId: "net.sourceforge.htmlunit",  artifactId: "htmlunit" })<--(av:ArtifactVersion {version:'2.35.0'})
MATCH (av)-[:DEPEND_ON_ARTIFACT_VERSION*]->(oav:ArtifactVersion)
WITH collect(oav) as nodes, a
UNWIND nodes as n
UNWIND nodes as m
WITH * WHERE id(n) < id(m)
WITH collect(n) + collect(m) as conflicts
UNWIND conflicts as nodes
RETURN (nodes)--(:Artifact)



Quellen:
https://stackoverflow.com/questions/14657265/finding-nodes-that-have-all-common-intermediaries
https://stackoverflow.com/questions/15437036/neo4j-find-children-derived-from-same-parents-in-cypher
https://stackoverflow.com/questions/21765875/cypher-subquery-on-each-node-in-path

match (c:JavaClass)-[:DEPEND_ON_EXTERNAL_JAVA_CLASS]->(d:JavaClass {name: 'org.apache.http.util.Args'}) return c,d

match (c:JavaClass)-[:DEPEND_ON_EXTERNAL_JAVA_CLASS]->(d:JavaClass) return c,d limit 20

match (n)-->(m) return n,m limit 20

match (n:ArtifactVersion {version: '4.5.8'}) return n

match (n:ArtifactVersion)-->(n) return n limit 20

MATCH (a:Artifact {  type: "jar",  groupId: "net.sourceforge.htmlunit",  artifactId: "htmlunit" })<--(av:ArtifactVersion {version:'2.35.0'})
RETURN a,av


MATCH (a:Artifact { artifactId: 'htmlunit', groupId: 'net.sourceforge.htmlunit', type: 'jar' } ) RETURN a

MATCH (n:Artifact)<--(a:ArtifactVersion)-[:DEPEND_ON_ARTIFACT_VERSION*]->(b:ArtifactVersion)-->(m:Artifact)
RETURN a, b, n, m

MATCH (n:Artifact)<--(a:ArtifactVersion)-->(b:ArtifactVersion)-->(m:Artifact)
RETURN a, b, n, m


-------------------------------------

# Variante b:
MATCH (a:Artifact {  type: "ear",  groupId: "com.charlyghislain.authenticator",  artifactId: "example-ear" })<--(av:ArtifactVersion {version:'1.0.6'})
MATCH (av)-[:DEPEND_ON_ARTIFACT_VERSION*]->(oav1:ArtifactVersion)
MATCH (av)-[:DEPEND_ON_ARTIFACT_VERSION*]->(oav2:ArtifactVersion)
WITH a,av,oav1,oav2
MATCH (oav1)-[:IS_VERSION_OF]->(conflict:Artifact)<-[:IS_VERSION_OF]-(oav2)
WHERE oav1 <> oav2
RETURN a,av,oav1,oav2,conflict


# Artifacts: ", "", "1.0.6
MATCH (a:Artifact {  type: "ear",  groupId: "com.charlyghislain.authenticator",  artifactId: "example-ear" })<--(av:ArtifactVersion {version:'1.0.6'})
MATCH (av)--(rv:ArtifactVersion)--(ra:Artifact)--(ag:ArtifactGroup)
RETURN a, av, ra, rv, ag

# 
MATCH (a:Artifact {  type: "ear",  groupId: "com.charlyghislain.authenticator",  artifactId: "example-ear" })<--(av:ArtifactVersion {version:'1.0.6'})
MATCH (av)-[:DEPEND_ON_ARTIFACT_VERSION*]->(oav:ArtifactVersion)-[:IS_VERSION_OF]->(oa:Artifact)
RETURN a, av, oav, oa

# virtual node:
RETURN apoc.create.vNode(["Regula"], {name: "Name"}) AS node
#
WITH apoc.create.vNode(["Regula"], {name: "Name"}) AS node1, apoc.create.vNode(["Regula"], {name: "Name"}) AS node2
RETURN node1, node2, apoc.create.vRelationship(node1, "IS_CONNECTED_TO", {}, node2) as relation

#
CALL apoc.nodes.group(['ArtifactVersion'],['groupId'], null, {selfRels:false})
YIELD node, relationship RETURN *;


--> https://stackoverflow.com/questions/53294084/aggregate-related-nodes-across-all-collections-in-neo4j-cypher