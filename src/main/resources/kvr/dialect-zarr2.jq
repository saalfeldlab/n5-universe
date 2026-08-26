# Dialect-specific accessors for Zarr v2 containers.
# Concatenated before common.jq and tree.jq; see JqTranslation.
#
# Every dialect file defines the same four filters, each taking a Directory node:
#   attrs         the node's user attributes (an object; {} if the node has none)
#   setAttrs(f)   update the node's user attributes with f
#   dsAttrs       the node's dataset metadata, or null if the node is not a dataset
#   dsDimensions  the dataset shape, slowest axis last, or null if not a dataset

def attrs: .attributes[".zattrs"] // {};

def setAttrs(f): .attributes[".zattrs"] |= f;

def dsAttrs: .attributes[".zarray"];

# NB: zarr .zarray/shape is C-order, N5 dimensions are F-order. Reversed here so
# that everything downstream sees one convention.
def dsDimensions: dsAttrs | if . == null then null else (.shape | reverse) end;
