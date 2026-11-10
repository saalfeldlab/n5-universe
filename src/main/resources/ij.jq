# jq definitions for IJ metadata

def isIJ: isDataset and (attrs | has("pixelWidth") and has("pixelHeight") and has("pixelUnit") and has("xOrigin") and has("yOrigin"));

def ijGetScale: [.pixelWidth, .pixelHeight, 1, .pixelDepth, .frameInterval];

def ijGetTranslation: [.xOrigin, .yOrigin, 0, .zOrigin, 0];

def ijGetUnits: [.unit, .unit, "", .unit, ""]

def ijGetAxes: .pixelUnit as $unit | ijDimensions as $czt |
     [  "x", "y" ]
     | if ($czt | .[0]) > 1 then . + ["c"] else . end
     | if ($czt | .[1]) > 1 then . + ["z"] else . end
     | if ($czt | .[2]) > 1 then . + ["t"] else . end;

def ijDimensionsSafe: . as $this | [1,1,1]
    | if ( $this.numChannels > 1 ) then .[0] = $this.numChannels else . end
    | if ( $this.numSlices > 1 ) then .[1] = $this.numSlices else . end
    | if ( $this.numFrames > 1 ) then .[2] = $this.numFrames else . end;

def ijDimensions: [.numChannels, .numSlices, .numFrames];

