# OME-NGFF 0.6-dev4 Refactor Progress

## Goal

Update `byDimension` coordinate transform support from the old spec (axes identified by
**name**) to the new spec (axes identified by **integer index**).

A conformance test in `NgffTransformsConformance` verifies the result:

```
zarr: /home/john/dev/ngff/ome_zarr_transformations_conformance/cases/byDimension.ome.zarr
input space:  "input"
output space: "output"
input coords: [[1,2]]   (y=1, x=2)
expected:     [1, 20]   (identity on y, scale×10 on x)
```

---

## Compilation Fixes

The change from `String[]` to `int[]` for axis indices in `AbstractCoordinateTransform`
(fields `inputAxes`, `outputAxes`) broke several files.

### `ByDimensionCoordinateTransform`
- `inputAxesFromTransforms()` / `outputAxesFromTransforms()`: switched from `flatMap` +
  `String[]` to `flatMapToInt` + `int[]`
- `createPreTransform()`: replaced `AxisUtils.findPermutation(String[], String[])` with
  direct use of the index array as the `RealComponentMappingTransform` mapping
- `createPostTransform()`: replaced string-based permutation with an inverse-permutation
  built from the index array
- `validate()`: replaced name-membership checks with integer bounds checks
- Removed `AxisUtils` import, `contains()`, and `firstIndexOf()` helpers
- Added `getTransformations()` accessor

### `StackedCoordinateTransform`
- `inputAxesLabels()` / `outputAxesLabels()`: fixed with `flatMapToInt`
- `buildTransform()`: replaced string-based permutation logic with same index-based
  approach used in `ByDimensionCoordinateTransform`
- Removed `AxisUtils` import

### `SequenceCoordinateTransform`
Internal utility methods (`isValid`, `axisOrdersForTransform*`, `cumNeededAxes`,
`inputIndexesFromAxisOrders`, `outputIndexesFromAxisOrders`) updated from `String[]`/
`HashSet<String>` to `int[]`/`HashSet<Integer>` throughout. Added `boxedList()` helper.
These are dead/experimental code not called from anywhere else.

### `CoordinateSystems`
- `getInputAxes(t)` / `getOutputAxes(t)`: changed return type from `String[]` to `int[]`
- `outputIsSubspace()` / `outputIsSuperspace()`: removed `int[]` branch (can't resolve
  without parent space context); retained named-space branch
- `outputHasAxis()` / `outputMatchesAny()`: same — now route exclusively through the
  named output space

### `TransformGraph`
- Removed dead `containsAny` checks in `buildTransformFromAxes` whose sets (`outAxes`,
  `inAxes`) were never populated (the `addAll` calls were already commented out)

---

## Deserialization Fixes

### `CoordinateTransformAdapter`
- Added `case("byDimension")`: mirrors the `sequence` case — manually deserializes the
  `transformations` array through the adapter so type dispatch and axis injection apply
  recursively to each sub-transform
- Added post-switch block that runs for **any** deserialized transform: reads `input_axes`
  and `output_axes` JSON fields (snake_case, per spec) and calls the new setters

### `AbstractCoordinateTransform`
- Added `setInputAxes(int[])` and `setOutputAxes(int[])` setters to support the
  post-switch injection above

### `ByDimensionCoordinateTransform`
- Added `ByDimensionCoordinateTransform(String name, String input, String output,
  CoordinateTransform<?>... transformations)` constructor for use during deserialization
  (validation deferred until `CoordinateSystem` objects are resolved)

---

## Tests Added

`NgffTransformDeserializationTest.testSubTransformAxesDeserialization`  
Deserializes a `byDimension` JSON blob and asserts that `input_axes` / `output_axes` are
correctly populated on each sub-transform. **Passes.**

---

## Current Status

The conformance test now outputs `[0.0, 20.0]` instead of the expected `[1.0, 20.0]`.

- The **scale×10** on axis 1 (`x: 2→20`) works correctly.
- The **identity** on axis 0 (`y`) outputs `0` instead of `1`.

### Root cause

`IdentityCoordinateTransform.getTransform()` returns an empty
`InvertibleRealTransformSequence`, which reports **0 source/target dimensions**.
`StackedRealTransform` therefore skips it entirely, leaving the output slot for `y` at
its default value of `0.0`.

### Next step

Fix `IdentityCoordinateTransform.getTransform()` to return an n-dimensional identity
(e.g. `new AffineTransform(n)`) where `n = inputAxes.length`.
