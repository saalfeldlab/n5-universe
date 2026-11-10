# Shared jq definitions for translated containers: values only.
# They work on plain json (attribute objects, transforms, axes, affines, scale arrays)
# and know nothing about Directory nodes or the dialect accessors. Definitions that
# take a node or a tree belong in tree.jq.
# Concatenated after dialect-<x>.jq and before tree.jq; see JqTranslation.

def cleanPath: . | sub("^\\.(?<x>.*)\\/.+"; "\(.x)"; "g");

def emptyIfNull: if (. == null) then empty else . end;

def id3d: [[1,0,0,0], [0,1,0,0], [0,0,1,0]];

def id2d: [[1,0,0], [0,1,0]];

def id3dflat: [1,0,0,0, 0,1,0,0, 0,0,1,0];

def id2dflat: [1,0,0, 0,1,0];

def permute( $arr; $indexes ):
   if all( (( $arr | type ) == "array" ); ( ($indexes | type) == "array"  )) then
       reduce ($indexes | .[]) as $i ( []; . + [ $arr | .[$i]])
   else null end;

def affineFromScaleAndFactors( $s; $f ):
  if ( $s | length) == 2 then [ ($s[0]*$f[0]), 0, ($f[0]-1)/2.0, 0, ($s[1]*$f[1]), ($f[1]-1)/2.0 ]
  elif ($s | length) == 3 then [($s[0]*$f[0]), 0, 0, ($f[0]-1)/2.0, 0, ($s[1]*$f[1]), 0, ($f[1]-1)/2.0, 0, 0,  ($s[2]*$f[2]), ($f[2]-1)/2.0 ]
  else null end;

def affineFromScaleAndFactorsArr:
  .[0] as $s | .[1] as $f |
  if ( $s | length) == 2 then [ ($s[0]*$f[0]), 0, ($f[0]-1)/2.0, 0, ($s[1]*$f[1]), ($f[1]-1)/2.0 ]
  elif ($s | length) == 3 then [($s[0]*$f[0]), 0, 0, ($f[0]-1)/2.0, 0, ($s[1]*$f[1]), 0, ($f[1]-1)/2.0, 0, 0,  ($s[2]*$f[2]), ($f[2]-1)/2.0 ]
  else null end;

def scaleOffsetFromScaleAndFactorsArr:
  .[0] as $s | .[1] as $f |
  if ( $s | length) == 2 then [ [($s[0]*$f[0]), ($s[1]*$f[1])], [ ($f[0]-1)/2.0,  ($f[1]-1)/2.0 ] ]
  elif ($s | length) == 3 then [ [($s[0]*$f[0]), ($s[1]*$f[1]), ($s[2]*$f[2])], [($f[0]-1)/2.0, ($f[1]-1)/2.0, ($f[2]-1)/2.0 ]]
  else null end;

def applyDownsamplingToFlatAffine( $a; $f ):
  if ( $s | length) == 2 then [ ($s[0]*$f[0]), 0, ($f[0]-1)/2.0, 0, ($s[1]*$f[1]), ($f[1]-1)/2.0 ]
  elif ($s | length) == 3 then [($s[0]*$f[0]), 0, 0, ($f[0]-1)/2.0, 0, ($s[1]*$f[1]), 0, ($f[1]-1)/2.0, 0, 0,  ($s[2]*$f[2]), ($f[2]-1)/2.0 ]
  else null end;

def arrMultiply( $s1; $s2 ): [$s1, $s2] | transpose | map(.[0] * .[1]) ;

def axisTypeFromLabel( $labRaw ):
    $labRaw | ascii_downcase as $lab |
    if $lab == "x" then "space"
    elif $lab == "y" then "space"
    elif $lab == "z" then "space"
    elif $lab == "t" then "time"
    elif $lab == "c" then "channel"
    else "unknown" end;

def intensityRange( $min; $max ): { "intensityLimits" : {
    "min" : $min,
    "max" : $max
    }
};

def rgbaColor( $r; $g; $b; $a ): {
    "red" : $r,
    "green" : $g,
    "blue" : $b,
    "alpha" : $a
};

def intColor( $rgba ): {
    "rgba" : $rgba
};

def affineDiagonalIndexes( $nd ):  [range( $nd )] | map( (. * ($nd + 1)) + . );

def affineTranslationIndexes( $nd ):  [range($nd)] | map( ($nd+1) * . + $nd);

