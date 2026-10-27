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

def attrHasTform: attrs | has("spatialTransform");

def numTformChildren: .children | reduce (keys| .[]) as $k (
    [.,0];
    [  .[0],
       if (.[0] | .[$k] | attrHasTform) then .[1] + 1 else .[1] end ])
      | .[1];

def isIJ: isDataset and (attrs | has("pixelWidth") and has("pixelHeight") and has("pixelUnit") and has("xOrigin") and has("yOrigin"));

# def ijAffine2d3d:
#     . as $this |
#     if ( .dimensions | length ) == 2 then
#         id2d | setScale2d( [$this.pixelWidth, $this.pixelHeight] ) | setTranslation2d([ $this.xOrigin, $this.yOrigin] )
#     elif ( .dimensions | length ) == 3 then
#         id3d | setScale3d( [$this.pixelWidth, $this.pixelHeight, $this.pixelDepth]) | setTranslation3d([ $this.xOrigin, $this.yOrigin, $this.zOrigin])
#     else null end;

def ijAffineNd: attrs as $this | numDimensions as $nd | ($this | ijDimensions) as $czt
    | identityAsFlatAffine($nd)
    | setFlatAffine( $this.pixelWidth; $nd; 0; 0 )
    | setFlatAffine( $this.xOrigin; $nd; 0; $nd )
    | setFlatAffine( $this.pixelHeight; $nd; 1; 1 )
    | setFlatAffine( $this.yOrigin; $nd; 1; $nd )
    | [2, .]
    | if ($czt | .[0]) > 1 then [ .[0] +1, .[1] ] else . end
    | if ($czt | .[1]) > 1 then
        .[0] as $i | .[1] | setFlatAffine( $this.pixelDepth; $nd; $i; $i) | setFlatAffine( $this.zOrigin; $nd; $i; $nd)
        | [ $i +1, . ]
        else . end
    | if ($czt | .[2]) > 1 then
        .[0] as $i | .[1] | setFlatAffine( $this.frameInterval; $nd; $i; $i) | [ $i +1, . ]
        else . end
    | .[1];

def ijToTransform: ([ijAffineNd, null] | arrayAndUnitToTransform) as $transform |
    (attrs | ijAxes) as $axes | setAttrs( . + $transform + { axes: $axes } );

def hasMultiscales: type == "object" and has("children") and ( numTformChildren > 1 );

# def buildMultiscale: [(.children | keys | .[]) as $k | .children |  .[$k].attributes + {"path": $k } ];

def buildMultiscaleST: [(.children | keys | .[]) as $k | .children |  {"path": $k, "spatialTransform" : (.[$k] | attrs.spatialTransform) }];

# def addMultiscale: buildMultiscale as $ms | .path as $p | .attributes |= . + { "multiscales": { "datasets": $ms , "path": $p }};

# def addAllMultiscales: walk( if hasMultiscales then addMultiscale else . end );

# def buildMultiChannelFull: [(.children | keys | .[]) as $k | .children |  ( .[$k].attributes ) ];

def isMultiChannel: type == "object" and has("children") and ( .children | has("c0"));

def buildMultiChannel: [(.children | keys | .[]) as $k | .children |  {"path": $k } ];

# def addMultiChannelFull: buildMultiChannelFull as $ms | .path as $p | .attributes |= . + { "multichannel": { "datasets": $ms , "path": $p }};

def addMultiChannel: buildMultiChannel as $ms | .path as $p | setAttrs( . + { "multichannel": { "datasets": $ms , "path": $p }} );

# def addAllMultichannelFull: walk( if isMultiChannel then addMultiChannelFull else . end );

def addAllMultichannel: walk( if isMultiChannel then addMultiChannel else . end );

def treeEditAttrs( $path; f ): getSubTree( $path ) |= setAttrs( f );

def treeAddAttrs( $path; $attrs ): treeEditAttrs( $path; . + $attrs );

def isNgffMultiscale:
    type == "object" and
    (attrs | .multiscales | type == "array" and length > 0 and (.[0] | has("datasets")));

# def ngffAddTransformsToChildren( $unit; $i; $rev ):
#     .children as $children |
#     (.attributes | .multiscales | .[$i]) as $ms |
#     ( $ms | ngffTransformsFromMultiscale($unit; $i; $rev) ) as $transforms |
#     ( $ms | .datasets | map (.path)) as $paths |
#     ( reduce ($paths | .[] ) as $p (
#         $children;
#         (.[$p] | .attributes) |= . + ( $transforms | .[$p]) )) as $newChildren |
#     .children |= $newChildren;

# def ngffAddTransformsToMultiscale( $unit; $i; $rev ):
#     (.attributes | .multiscales | .[$i]) as $ms |
#     ( $ms | ngffTransformsFromMultiscale($unit; $i; $rev) ) as $transforms |
#     ( $ms | .datasets | map (.path)) as $paths |
#     ( reduce ($paths | .[] ) as $p (
#         $children;
#         (.[$p] | .attributes) |= . + ( $transforms | .[$p]) )) as $newChildren |
#     .children |= $newChildren;

# def ngffAddTransformsToMultiscales( $unit; $i; $rev ):
#     (.attributes | .multiscales | .[$i]) as $ms |
#     ( .attributes | .multiscales | .[$i] | .datasets ) as $dsets |
#     ( $ms | ngffTransformsFromMultiscale($unit; $i; $rev)) as $transforms |
#     ( $dsets | map ( .path as $p | . + ( $transforms | .[$p]) )) as $newdsets |
#     setpath( ["attributes","multiscales",0,"datasets"]; $newdsets );

def selectMultiscale( $i ):
    setAttrs( with_entries( if .key == "multiscales" then .value |= ( .[$i]) else . end ) );

def backupNgffMultiscales( $newName) :
    setAttrs( with_entries( .key |= if . == "multiscales" then $newName else . end ) );

# def convertNgff( $unit; $i; $rev ):
#     ngffAddTransformsToChildren( $unit; $i; $rev ) | ngffAddTransformsToMultiscales( $unit; $i; $rev ) | selectMultiscale( $i );

# def can2NgffGetScale: .attributes | .spatialTransform | .transform | .scale ;

# def can2NgffGetDownsampleFactors : .children | map( can2NgffGetScale) |
#     if (. | length) > 1 then
#         [.[0], .[1]] | transpose | map ( .[1] / .[0] )
#     else [1,1,1] end;

# def setNgffScaleMetadata: can2NgffGetDownsampleFactors as $scale |
#     ( .attributes | .multiscales | .[0] ) |= . + buildNgffScaleMetadata( $scale );

# def requiredDatasetAttributes : ["dimensions","dataType","blockSize","compression"];

# def clearDatasetMetadata: requiredDatasetAttributes as $required |
#     reduce ( keys | .[] ) as $k ( . ;
#         if ( $required | contains([$k]) | not) then del(.[$k]) else . end );
