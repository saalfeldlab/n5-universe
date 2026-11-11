# jq definitions for NGFF-v0.4

def hasMultiscales: type == "object" and has("children") and ( numTformChildren > 1 );

def ngffIsMultiscale:
    type == "object" and
    (attrs | .multiscales | type == "array" and length > 0 and (.[0] | has("datasets")));

# -- on one entry of multiscales[].datasets --

# The first transformation of $type in the dataset's coordinateTransformations, or null.
def ngffDsTransform( $type ):
    ((.coordinateTransformations // []) | map(select(.type == $type)) | .[0]);

# Scale of the dataset. [] if there is none.
def ngffDsScale: ngffDsTransform("scale") | (.scale // []);

# Translation of the dataset. Zeros, as long as the scale, if there is none.
def ngffDsTranslation: ngffDsScale as $s | ngffDsTransform("translation") | (.translation // ($s | map(0)));

# -- on the base node of a multiscales group --
# These read the first dataset (highest resolution) of the first multiscales entry.

def ngffGetScale: attrs | (.multiscales[0].datasets[0] // {}) | ngffDsScale;

def ngffGetTranslation:
    attrs | (.multiscales[0].datasets[0] // {}) | ngffDsTranslation;

# Transformations

def scale( $s ): { "type": "scale", "scale": $s };

def translation( $t ): { "type": "translation", "translation": $t };

def addInput( $in): . + {"input": $in};

def addOutput($out): . + {"output": $out};

# Builders

# Inverse of n5vGetMultiscales, on the base node of a multiscales group: given $ms as
# returned by n5vGetMultiscales ([{path, scale, translation, units}, ...]), sets
# multiscales[0] (version "0.4") from it. Other fields of an existing multiscales[0]
# (name, type, metadata, ...) are kept.
#   datasets: for each entry, { path, coordinateTransformations: [scale, translation] }.
#             The translation is left out if the entry has none.
#   axes:     $axes, as given; 
def ngffSetMultiscale( $ms; $axes ):
    (attrs | .multiscales[0] // {}) as $old
    | ($ms | map( { "path": .path,
                    "coordinateTransformations":
                        ( [ scale(.scale) ] + (if .translation == null then [] else [ translation(.translation) ] end) ) } )) as $datasets
    | setAttrs( . + { "multiscales": [ $old + { "version": "0.4", "axes": $axes, "datasets": $datasets } ] } );


# From the base node, clear the multiscales from the attributes.
def ngffMultiscaleClear:
    if (attrs | has("multiscales")) then setAttrs(del(.multiscales)) else . end;

def ngffAxis( $n; $t; $u ): { name : $n, type : $t, unit : $u };

# Create an ngff axis array from an array of names, and a single unit (applied to spatial
# axes only). The type of each axis comes from its name (axisTypeFromLabel): "x", "y", "z"
# are space, "t" time, "c" channel (case insensitive), anything else "unknown".
def ngffAxesFromNames( $names; $spaceUnit ):
    $names | map( axisTypeFromLabel(.) as $t
        | if $t == "space" then ngffAxis(.; $t; $spaceUnit) else { "name": ., "type": $t } end );

# Getters

# On the base node of a multiscales group. Reads multiscales[0] and returns an array, in
# the order of its datasets, of what n5vGetMultiscale returns:
# [ { path, scale, translation (zeros if there is none), units }, ... ]
# units has one element per axis ("pixel" for an axis without a unit, e.g. channel).
# Scales and translations are in the order of the axes. Top-level
# coordinateTransformations of the multiscales entry are not applied.
def ngffGetMultiscale:
    (attrs | .multiscales[0] // {}) as $ms
    | (($ms.axes // []) | map(.unit // "pixel")) as $units
    | ($ms.datasets // [])
    | map( { "path": .path, "scale": ngffDsScale, "translation": ngffDsTranslation, "units": $units } );

def ngffAxesFromMultiscale( $unit; $rev ): ( .axes | if $rev then reverse else . end) as $axLabels | axesFromLabels($axLabels; $unit );


# Builders

def buildNgffScaleMetadata( $scale ): {
    "metadata": {
        "order":0,
        "preserve_range" : true,
        "scale": $scale }
};

def selectMultiscale( $i ):
    setAttrs( with_entries( if .key == "multiscales" then .value |= ( .[$i]) else . end ) );

