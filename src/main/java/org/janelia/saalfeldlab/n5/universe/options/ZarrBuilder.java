package org.janelia.saalfeldlab.n5.universe.options;

public abstract class ZarrBuilder extends AbstractN5Builder {

    protected String dimensionSeparator;

    ZarrBuilder(AbstractN5Builder sharedOptions, String dimensionSeparator) {
        super(sharedOptions);
        this.dimensionSeparator = dimensionSeparator;
    }

    public String getDimensionSeparator() {
        return dimensionSeparator;
    }

    public abstract ZarrBuilder dimensionSeparator(String dimensionSeparator);

}
