# jq definitions for COSEM metadata

def isCosem: type == "object" and has("transform") and (.transform | type == "object") and  (.transform | has("axes") and has("scale") and has("translate") and has("units") );

def cosemGetScale: .transform.scale;  

def cosemGetTranslation: .transform.translate;  

def cosemGetAxes: .transform.axes;  

def cosemGetUnits: .transform.units;  

def cosemAxisIndexes: {"axisIndexes":[ (.axes | index("x")) , (.axes | index("y")), (.axes | index("z")) ]};
