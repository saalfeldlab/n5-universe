[![](https://github.com/saalfeldlab/n5-universe/actions/workflows/build-main.yml/badge.svg)](https://github.com/saalfeldlab/n5-universe/actions/workflows/build-main.yml)

# n5-universe

Functionality shared across all N5.

Also includes:

- **`N5Factory`**: opens an `N5Reader`/`N5Writer` from a URI or path. It infers the storage format (N5, Zarr v2/v3, HDF5), and backend (Filesystem, AWS S3, Google cloud storage, HTTP)
- **`N5FactoryWithCache`**: a thread-safe `N5Factory` that reuses readers and writers, keyed by normalized URI.
- **Metadata discovery**: `N5DatasetDiscoverer` walks a container and parses each node's metadata. Built-in parsers cover OME-NGFF multiscales (v0.3–v0.5), N5 Viewer, and COSEM. 
- **Coordinate transformations**: turns NGFF transformations into imglib2 transforms.
- **Metadata translation**: `TranslatedN5Reader` and `TranslatedN5Writer` present a container's metadata through jq-style rewriting, without changing the stored data.
