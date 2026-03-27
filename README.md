# FlowPath - qUMAP

UMAP dimensionality reduction and visualization for multiplexed imaging data in [QuPath](https://qupath.github.io/).

Part of the [FlowPath extension suite](https://github.com/sceriff0/flowpath-catalog).

## Features

- **UMAP Visualization**: Compute and display 2D UMAP embeddings from all available markers
- **Phenotype Coloring**: Automatically colors cells by their PathClass (e.g., from FlowPath - GatingTree)
- **Interactive Polygon Gating**: Draw polygons in UMAP space to select cell populations
  - Cells outside the polygon are temporarily marked as UNFOCUSED
  - Tag selected populations with a name and color (permanent derived PathClass)
  - Multiple population tags can coexist
- **Marker Expression Overlay**: Side-by-side UMAP colored by z-score or raw marker intensity
- **Performance**: Configurable subsampling for large datasets, OOM protection
- **Export**: UMAP coordinates to CSV

## Requirements

- QuPath 0.7.0+
- Java 25+

## Installation

Download the latest JAR from [Releases](https://github.com/sceriff0/qupath-extension-qumap/releases) and drag it into QuPath.

Or add the [FlowPath catalog](https://github.com/sceriff0/flowpath-catalog) to QuPath's Extension Manager.

## Build

```bash
JAVA_HOME=/path/to/jdk25 ./gradlew build
```
