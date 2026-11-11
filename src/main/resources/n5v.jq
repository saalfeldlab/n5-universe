# jq definitions for n5-viewer metadata

def isN5V: type == "object" and has("pixelResolution");

def n5VisMultiChannel: type == "object" and has("children") and (.children | has("c0"));

def n5vIsMultiscale: type == "object" and has("children") and (.children | has("s0")) and
    (.children.s0 | attrs | isN5V);

def n5vIsResObjDs: has("downsamplingFactors") and has("pixelResolution") and (.pixelResolution | type == "object");

def n5vIsResObj: has("pixelResolution") and (.pixelResolution | type == "object");

def n5vIsResArrDs: has("downsamplingFactors") and has("pixelResolution") and (.pixelResolution | type == "array");

def n5vIsResArr: has("pixelResolution") and (.pixelResolution | type == "array");

# -- on the attributes of one dataset --

# Scale, from "pixelResolution" (either an array, or an object with "dimensions").
# [] if there is none.
def n5vScale:
    if n5vIsResObj then .pixelResolution.dimensions
    elif n5vIsResArr then .pixelResolution
    else [] end;

# Translation is always zero, with the same length as the scale.
def n5vTranslation: n5vScale | map(0);

def n5vUnit: (if n5vIsResObj then (.pixelResolution.unit // "pixel") else "pixel" end); 

# An array as long as the scale, where every element is the pixelResolution.unit
# ("pixel" if there is none, e.g. when pixelResolution is an array).
def n5vUnits: n5vUnit as $unit | n5vScale | map($unit);

# -- on the base node of a multiscale group (children s0, ..., sN) --
# These read the first scale level, s0.

def n5vGetScale: .children.s0 | attrs | n5vScale;

def n5vGetTranslation: .children.s0 | attrs | n5vTranslation;

def n5vGetUnit: .children.s0 | attrs | n5vUnit;

def n5vGetUnits: .children.s0 | attrs | n5vUnits;

# Axis names of the group, which n5v does not store: the defaults for spatial dialects
# (AxisUtils.defaultN5ViewerAxes), chosen by the number of dimensions of s0:
#   2: x y;  3: x y z;  4: x y z t;  5: x y z c t;  any other: [] (the Java version gives null).
def n5vGetAxisNames:
    (.children.s0 | dsDimensions | length) as $nd
    | if $nd == 2 then ["x", "y"]
      elif $nd == 3 then ["x", "y", "z"]
      elif $nd == 4 then ["x", "y", "z", "t"]
      elif $nd == 5 then ["x", "y", "z", "c", "t"]
      else [] end;

# Returns an array, ordered by scale level:
# [
#     { path: s0, scale: s0-scales, translation: s0-translations (zeros), units: s0-units},
#     ...,
#     { path: sN, scale: sN-scales, translation: sN-translations (zeros), units: sN-units}
# ]
def n5vGetMultiscale:
    .children
    | to_entries
    | map(select(.key | test("^s[0-9]+$")))
    | sort_by(.key[1:] | tonumber)
    | map( .key as $k | (.value | attrs) as $a
        | { "path": $k, "scale": ($a | n5vScale), "translation": ($a | n5vTranslation), "units": ($a | n5vUnits) } );

# Inverse of n5vGetMultiscale, on the base node of a multiscale group: given $ms as
# returned by n5vGetMultiscale, sets pixelResolution and downsamplingFactors on the
# child of each entry's path (merged into the attributes it already has).
#   pixelResolution:     { unit: units[0] (or "pixel"), dimensions: scale of s0 }
#   downsamplingFactors: scale / scale of s0, per dimension (downsamplingFactorsFromScales)
# translation is dropped: n5v has no way to store it.
# Entries are skipped if there is no child at their path, or if their scale is empty
# or does not have as many dimensions as the scale of s0.
def n5vSetMultiscale($ms):
    (($ms | map(select(.path == "s0")) | .[0]) // $ms[0]) as $base
    | reduce $ms[] as $e ( .;
        if ((.children // {}) | has($e.path)) and (($e.scale | length) > 0) and (($e.scale | length) == ($base.scale | length))
        then .children[$e.path] |= setAttrs( . + {
            "pixelResolution": { "unit": (($e.units // [])[0] // "pixel"), "dimensions": $base.scale },
            "downsamplingFactors": ($e.scale | downsamplingFactorsFromScales($base.scale))
        })
        else . end );

# From the base node, clear the pixelResolution from the attributes of all children
# Only the scale levels (children named s0, ..., sN) are touched.
def n5vMultiscaleClear:
    .children |= with_entries(
        if (.key | test("^s[0-9]+$")) then .value |= n5vMultiscaleLevelClear else . end );

# From a child scale node, clear the pixelResolution from the attributes 
def n5vMultiscaleLevelClear:
    if (attrs | has("pixelResolution") or has("downsamplingFactors"))
    then setAttrs(del(.pixelResolution, .downsamplingFactors))
    else . end;


