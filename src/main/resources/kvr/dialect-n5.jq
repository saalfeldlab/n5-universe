# Dialect-specific accessors for N5 containers.
# Concatenated before common.jq and tree.jq; see JqTranslation.
#
# Every dialect file defines the same four filters, each taking a Directory node:
#   attrs         the node's user attributes (an object; {} if the node has none)
#   setAttrs(f)   update the node's user attributes with f
#   dsAttrs       the node's dataset metadata, or null if the node is not a dataset
#   dsDimensions  the dataset shape, slowest axis last, or null if not a dataset

def attrs: .attributes["attributes.json"] // {};

def setAttrs(f): .attributes["attributes.json"] |= f;

def dsAttrs: attrs | if has("dimensions") and has("dataType") then . else null end;

def dsDimensions: dsAttrs | .dimensions;
