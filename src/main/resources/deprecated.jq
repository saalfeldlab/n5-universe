# jq definitions for the deprecated "canonical" metadata style

def arrayAndUnitToTransform: {
    "spatialTransform": {
        "transform" : {
            "type": "affine",
            "affine": .[0]
        },
        "unit": .[1]
    }
};

def arrayUnitToTransform($a;$u): {
    "spatialTransform": {
        "transform" : {
            "type": "affine",
            "affine": $a
        },
        "unit": $u
    }
};

def arrayUnitAxisToTransform($a;$u;$x): {
    "spatialTransform": {
        "transform" : {
            "type": "affine",
            "affine": $a
        },
        "unit": $u,
        "axes" : $x
    }
};

def canonicalAxis( $type; $lbl; $unit ): {
    "type" : $type,
    "label" : $lbl,
    "unit" : $unit };

def dataAxis( $i ): {
    "type" : "data",
    "label" : ("dim_" + ( $i | tostring)),
    "unit" : "none" };

def axisFromLabel( $lbl; $unit ):
    canonicalAxis( axisTypeFromLabel( $lbl ); $lbl; $unit );

def axesFromLabels( $lbls; $unit ): $lbls | map( axisFromLabel( .; $unit ));


def spatialTransformNdims:
    (.transform.type) as $type |
    if $type == "affine" then
        (.transform.affine | length) as $len |
        (if $len == 2 then 1
        elif $len == 6 then 2
        elif $len == 12 then 3
        else null end)
    elif ($type == "scale") or ($type == "scale_translation") then
        (.transform.scale | length)
    elif ($type == "translation") then
        (.transform.translation | length)
    else null end;

def scaleFromSpatialTransform: (.transform.type) as $type |
    spatialTransformNdims as $nd |
    if $type == "affine" then (.transform.affine[ (affineDiagonalIndexes($nd) | .[]) ])
    elif ($type == "scale") or ($type == "scale_translation") then .transform.scale
    else [range($nd)] | map(1) end;

def translationFromSpatialTransform: (.transform.type) as $type |
    spatialTransformNdims as $nd |
    if $type == "affine" then (.transform.affine[ (affineTranslationIndexes($nd) | .[]) ])
    elif ($type == "translation") or ($type == "scale_translation") then .transform.translation
    else [range($nd)] | map(0) end;

def canonicalToCosemTransform: .spatialTransform as $st |
    ($st | spatialTransformNdims) as $nd |
    {
        "transform" : {
            "units" : ( [range($nd)] | map( $st | .unit  )),
            "scale" : ( $st | scaleFromSpatialTransform),
            "translate" : ( $st | translationFromSpatialTransform),
            "axes" : [ "z", "y", "x" ]
        }
    };

def canonicalToN5v: .spatialTransform as $st |
    {
        "pixelResolution" : {
            "unit" : ( $st | .unit ),
            "dimension" : ( $st | scaleFromSpatialTransform)
        }
    };

def cosemToScaleOffsetSimple: { "spatialTransform": {
        "transform": {
            "type":"scale_offset",
            "scale" : [ .scale[.axisIndexes[0]],
                .scale[ .axisIndexes[1]],
                .scale[ .axisIndexes[2]] ],
            "offset" : [ .translate[.axisIndexes[0]],
                .translate[.axisIndexes[1]],
                .translate[.axisIndexes[2]] ]
        },
        "unit" : .units[0]
    }
};

def scaleOffsetToCosem: .spatialTransform | .unit as $u | .transform | {
    "scale" : permute( .scale; [2,1,0] ),
    "translate" : permute( .offset; [2,1,0]),
    "units" : [ $u, $u, $u ],
    "axes" : ["z", "y", "x" ]
};

def cosemToTransformSimple: { "spatialTransform": {
        "transform": {
            "type":"affine",
            "affine": [ .scale[.axisIndexes[0]], 0.0, 0.0, .translate[.axisIndexes[0]],
                        0.0, .scale[.axisIndexes[1]], 0.0, .translate[.axisIndexes[1]],
                        0.0, 0.0, .scale[.axisIndexes[2]], .translate[.axisIndexes[2]]]
        },
        "unit" : .units[0]
        }
    };

def cosemToTransform: (.transform |= . + cosemAxisIndexes) | . + (.transform | cosemToScaleOffsetSimple);

def ijAffine2d3d:
     . as $this |
     if ( .dimensions | length ) == 2 then
         id2d | setScale2d( [$this.pixelWidth, $this.pixelHeight] ) | setTranslation2d([ $this.xOrigin, $this.yOrigin] )
     elif ( .dimensions | length ) == 3 then
         id3d | setScale3d( [$this.pixelWidth, $this.pixelHeight, $this.pixelDepth]) | setTranslation3d([ $this.xOrigin, $this.yOrigin, $this.zOrigin])
     else null end;

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

def attrHasTform: attrs | has("spatialTransform");

def numTformChildren: .children | reduce (keys| .[]) as $k (
    [.,0];
    [  .[0],
       if (.[0] | .[$k] | attrHasTform) then .[1] + 1 else .[1] end ])
      | .[1];

# def buildMultiscale: [(.children | keys | .[]) as $k | .children |  .[$k].attributes + {"path": $k } ];

def buildMultiscaleST: [(.children | keys | .[]) as $k | .children |  {"path": $k, "spatialTransform" : (.[$k] | attrs.spatialTransform) }];

# def addMultiscale: buildMultiscale as $ms | .path as $p | .attributes |= . + { "multiscales": { "datasets": $ms , "path": $p }};

# def addAllMultiscales: walk( if hasMultiscales then addMultiscale else . end );

# def buildMultiChannelFull: [(.children | keys | .[]) as $k | .children |  ( .[$k].attributes ) ];

def buildMultiChannel: [(.children | keys | .[]) as $k | .children |  {"path": $k } ];

# def addMultiChannelFull: buildMultiChannelFull as $ms | .path as $p | .attributes |= . + { "multichannel": { "datasets": $ms , "path": $p }};

def addMultiChannel: buildMultiChannel as $ms | .path as $p | setAttrs( . + { "multichannel": { "datasets": $ms , "path": $p }} );

# def addAllMultichannelFull: walk( if isMultiChannel then addMultiChannelFull else . end );

def addAllMultichannel: walk( if isMultiChannel then addMultiChannel else . end );
