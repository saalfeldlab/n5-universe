# jq definitions for n5-viewer metadata

def isN5V: type == "object" and has("pixelResolution");

def n5vGetScale: [];

def n5vTransformArr: { "type":"affine", "affine": (.pixelResolution | affineFromScaleArray)};

def n5vTransformObj: { "type":"affine", "affine": (.pixelResolution.dimensions | affineFromScaleArray )};

def n5visResObjDs: has("downsamplingFactors") and has("pixelResolution") and (.pixelResolution | type == "object");

def n5visResObj: has("pixelResolution") and (.pixelResolution | type == "object");

def n5visResArrDs: has("downsamplingFactors") and has("pixelResolution") and (.pixelResolution | type == "array");

def n5visResArr: has("pixelResolution") and (.pixelResolution | type == "array");

def n5vToTransform: {
  "transform": {
    "type": "affine",
    "affine": [
            (if n5visResObj then .pixelResolution.dimensions elif n5visResArr then .pixelResolution else null end),
            (.downsamplingFactors // [1, 1, 1] )] |
            affineFromScaleAndFactorsArr
  },
  "unit" : (.pixelResolution.unit // "pixel")
};

def n5vToScaleOffset:
( [ (if n5visResObj then .pixelResolution.dimensions elif n5visResArr then .pixelResolution else null end),
(.downsamplingFactors // [1, 1, 1] )] | scaleOffsetFromScaleAndFactorsArr ) as $scaleOffset |
{
  "transform": {
    "type": "scale_offset",
    "scale": ($scaleOffset | .[0]),
    "offset": ($scaleOffset | .[1])
  }
};

def n5vToTransformF: . + {
  "transform": {
    "type": "affine",
    "affine":  affineFromScaleAndFactors(
            (if n5visResObj then .pixelResolution.dimensions elif n5visResArr then .pixelResolution else null end),
            (.downsamplingFactors // [1, 1, 1] ) )
  },
  "unit" : (.pixelResolution.unit // "pixel")
};

def scaleOffsetToN5v: .spatialTransform | .unit as $unit | .transform | {
    "pixelResolution" :  {
        "dimensions": .scale,
        "unit" : $unit
    }
};

def n5vToCanonicalAffine: . + { "spatialTransform" : n5vToTransform };

def n5vToCanonicalScaleOffset: . + { "spatialTransform" : n5vToScaleOffset };

def isMultiChannel: type == "object" and has("children") and ( .children | has("c0"));

