# jq definitions that take a Directory node or tree
#   { "attributes": { <file name>: <file content> }, "children": { <name>: <node> } }
# i.e. anything that touches .children, tree paths, or the dialect accessors (attrs,
# setAttrs, dsAttrs, dsDimensions). Definitions on plain json values belong in common.jq.
# Concatenated after dialect-<x>.jq and common.jq; see JqTranslation.
# NB: the commented-out defs below read `.attributes` as if it were the merged
# attribute map of the old ContainerMetadataNode, so on the current tree shape they
# would silently produce wrong results. They are commented out so that calling one
# fails to compile instead, and are to be rewritten onto attrs/setAttrs/dsAttrs (and
# re-enabled) as they are needed. Defs that only serve a commented-out def are
# commented out with it. The active defs are tested in TreeDefinitionTests.

def forAllNodes(f): f | if has("children") then .children |= map_values(forAllNodes(f)) else . end;

def toTreePath: ltrimstr( "/") | split("/") | map_values( ["children", . ] ) | flatten;

def fromTreePath: if (length == 0) then "" else [.[range(1;length;2)]] | join("/") end;

def getSubTree( $path ): getpath( $path | toTreePath );

def moveSubTree( $srcPath; $dstPath ): getSubTree( $srcPath ) as $subTree | setpath( $dstPath | toTreePath; $subTree )
    | delpaths([$srcPath | toTreePath]);

def isDataset: type == "object" and dsAttrs != null;

def hasAttributes: type == "object" and has("attributes");

def numDimensions: dsDimensions | length;

def hasDims: dsDimensions != null;

def flattenTree: .. | select( type == "object" and has("path")) | del(.children);

def nodePaths: [] , ( (.children // {}) | keys_unsorted[] as $k | [ "children", $k ] + ( .[$k] | nodePaths ) );

def addPaths: reduce nodePaths as $p ( . ; setpath( $p + ["path"]; ($p | fromTreePath) ) );

def treeEditAttrs( $path; f ): getSubTree( $path ) |= setAttrs( f );

def treeAddAttrs( $path; $attrs ): treeEditAttrs( $path; . + $attrs );

