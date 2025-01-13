# SegmentEventClient

## Design Preview
![Design Preview](assets/SegmentEvent-V2.drawio.svg)

## Installation
```xml
<dependencies>
    <!-- not spring framework -->
    <dependency>
        <groupId>com.pacvue</groupId>
        <artifactId>segment-event-core</artifactId>
        <version>${project.version}</version>
    </dependency>
    
    <!-- spring framework, and not springboot -->
    <dependency>
        <groupId>com.pacvue</groupId>
        <artifactId>segment-event-spring</artifactId>
        <version>${project.version}</version>
    </dependency>
    
    <!-- springboot framework -->
    <dependency>
        <groupId>com.pacvue</groupId>
        <artifactId>segment-event-springboot</artifactId>
        <version>${project.version}</version>
    </dependency>
</dependencies>
```

## How to initialize
[ServerConfiguration](https://github.com/sheldon-pacvue/segmenet-event-client/blob/main/segment-event-example/src/main/java/com/pacvue/segment/event/example/configuration/ServerConfiguration.java)

## How to use
```java
private SegmentIO segmentIO;

public void someMethod() {
    segmentIO.track(() -> Mono.just(TrackMessage.builder("event-name")
            .userId("userId")
            .anonymousId("anonymousId")
            .sentAt(new Date())));
}
```

## Dependency
[![JDK 17-21](https://img.shields.io/badge/jdk-v17__21-blue)](https://javaalmanac.io/jdk/17/)