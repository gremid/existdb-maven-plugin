# Quick Guide

This plugin supports the development of eXist-db/XAR applications, allowing to roundtrip between 
the application running in a database instance and its sources as a Maven module in the filesystem.

## Maven Module Setup

Configure the project's Maven repository in your POM:

    <pluginRepositories>
        <pluginRepository>
            <id>gremid-existdb</id>
            <name>existdb-maven-plugin Repository</name>
            <url>https://raw.github.com/gremid/existdb-maven-plugin/mvn-repo/</url>
        </pluginRepository>
    </pluginRepositories>

Also configure the plugin as part of your build:

    <build>
        <plugins>
            <plugin>
                <groupId>net.middell</groupId>
                <artifactId>existdb-maven-plugin</artifactId>
                <version>...</version>
                <extensions>true</extensions>
                <configuration>
                    ...
                </configuration>
            </plugin>
        </plugins>
    </build>

Setting `<extensions/>` to `true` registers the plugin's custom lifecycle for XAR packaging:

    <groupId>...</groupId>
    <artifactId>...</artifactId>
    <version>...</version>
    <packaging>xar</packaging>
    
In the `package` lifecycle phase, the plugin assembles all resources as processed by
[Maven's resources plugin](https://maven.apache.org/plugins/maven-resources-plugin/)
and builds a XAR archive in the output directory (`target/` by default). E.g.
 
    <build>
         <resources>
             <resource>
                 <directory>src/main/xml</directory>
             </resource>
             <resource>
                 <directory>src/main/xar</directory>
                 <filtering>true</filtering>
             </resource>
         </resources>
     </build>
     
assembles resources from `src/main/xml` and filtered ones from `src/main/xar` when calling
 
    $ mvn package
    
with the plugin registered.

## Plugin Configuration

The plugin provides 2 goals:

1. `existdb:xar`: Normally called as part of the `package` lifecycle phase, this goal assembles
   resources in a XAR archive.
1. `existdb:sync`: Not part of any lifecycle phase by default, this goal can be used to synchronize
   the state of an application's resources in an eXist-db instance with sources in the local
   filesystem.
   
While the `xar` goal does not offer extensive configuration beyond what Maven already offers for
processing resources, the `sync` goal is adjustable to a project's resource layout in eXist and
the module. See the [plugin documentation](plugin-info.html) for details and an example.

## Development workflow

With both goals of the plugin set up, iterative development in eXist *and* in the filesystem becomes
possible by

1. setting up the resources in a Maven module,
1. packaging resources and descriptors as a XAR archive and deploy the archive to eXist,
1. running, testing and editing resources within eXist, possibly via its own IDE "eXide",
1. synchronizing the state of the resources in eXist with the one in the filesystem via
   `existdb:sync`,
1. running, testing and editing resources in the filesystem, optionally putting them in a VCS,
1. ... repeat steps 2-5 as needed ...
