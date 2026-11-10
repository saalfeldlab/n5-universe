# jq definitions for NGFF-v0.4

def hasMultiscales: type == "object" and has("children") and ( numTformChildren > 1 );

def isNgffMultiscale:
    type == "object" and
    (attrs | .multiscales | type == "array" and length > 0 and (.[0] | has("datasets")));

def ngffGetScales: null; # TODO 

def ngffGetTranslation: null; # TODO 

# Transformations

def scale( $s ): { "type": "scale", "scale": $s };

def translation( $t ): { "type": "translation", "translation": $t };

def addInput( $in): . + {"input": $in};

def addOutput($out): . + {"output": $out};

# Builders

def ngffBuildMultiscales( $paths, $scales ): {} # TODO

def axis( $n; $t; $u ): { name : $n, type : $t, unit : $u };

# Getters

def ngffAxesFromMultiscale( $unit; $rev ): ( .axes | if $rev then reverse else . end) as $axLabels | axesFromLabels($axLabels; $unit );

def ngffTransformsFromMultiscale( $unit; $i; $rev ):
    ngffAxesFromMultiscale( $unit; $rev ) as $axes |
    (.metadata | .scale | if $rev then reverse else . end) as $scales |
    reduce (.datasets | .[]) as $d (
        [ {}, $scales, $scales ];
        [ .[$i] + { ($d | .path) : { "spatialTransform" : { "unit" : $unit, "axes": $axes, "transform": scaleTransform(.[1])}} },
        arrMultiply( .[1]; .[2]),
        .[2] ])
    | .[0];

def buildNgffScaleMetadata( $scale ): {
    "metadata": {
        "order":0,
        "preserve_range" : true,
        "scale": $scale }
};

def selectMultiscale( $i ):
    setAttrs( with_entries( if .key == "multiscales" then .value |= ( .[$i]) else . end ) );



## TODO decide on details of this
#def getMultiscale: 
#[
#    {
#        path: "s0"
#        scale:  "111
#        trans 000
#    }
#]




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

# def setNgffScaleMetadata: can2NgffGetDownsampleFactors as $scale |
#     ( .attributes | .multiscales | .[0] ) |= . + buildNgffScaleMetadata( $scale );

# def convertNgff( $unit; $i; $rev ):
#     ngffAddTransformsToChildren( $unit; $i; $rev ) | ngffAddTransformsToMultiscales( $unit; $i; $rev ) | selectMultiscale( $i );

# def can2NgffGetScale: .attributes | .spatialTransform | .transform | .scale ;

# def can2NgffGetDownsampleFactors : .children | map( can2NgffGetScale) |
#     if (. | length) > 1 then
#         [.[0], .[1]] | transpose | map ( .[1] / .[0] )
#     else [1,1,1] end;

