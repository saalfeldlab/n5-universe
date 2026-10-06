package org.janelia.saalfeldlab.n5.universe.metadata.axes;

import java.util.Objects;

/**
 * The orientation of a spatial {@link Axis}, e.g.
 * {@code {"type": "anatomical", "value": "left-to-right"}}.
 * <p>
 * See <a href="https://ngff.openmicroscopy.org/rfc/4/index.html">NGFF RFC-4</a>.
 */
public class Orientation {

	public static final String ANATOMICAL = "anatomical";

	/*
	 * Allowed values for orientations of type "anatomical".
	 *
	 * see https://ngff.openmicroscopy.org/rfc/4/index.html
	 */
	public enum Anatomical {

		LEFT_TO_RIGHT("left-to-right"), RIGHT_TO_LEFT("right-to-left"),
		PROXIMAL_TO_DISTAL("proximal-to-distal"), DISTAL_TO_PROXIMAL("distal-to-proximal"),

		ANTERIOR_TO_POSTERIOR("anterior-to-posterior"), POSTERIOR_TO_ANTERIOR("posterior-to-anterior"),
		INFERIOR_TO_SUPERIOR("inferior-to-superior"), SUPERIOR_TO_INFERIOR("superior-to-inferior"),
		DORSAL_TO_PALMAR("dorsal-to-palmar"), PALMAR_TO_DORSAL("palmar-to-dorsal"),
		DORSAL_TO_PLANTAR("dorsal-to-plantar"), PLANTAR_TO_DORSAL("plantar-to-dorsal"),

		ROSTRAL_TO_CAUDAL("rostral-to-caudal"), CAUDAL_TO_ROSTRAL("caudal-to-rostral"),
		CRANIAL_TO_CAUDAL("cranial-to-caudal"), CAUDAL_TO_CRANIAL("caudal-to-cranial"),
		DORSAL_TO_VENTRAL("dorsal-to-ventral"), VENTRAL_TO_DORSAL("ventral-to-dorsal"),

		SUPERFICIAL_TO_DEEP("superficial-to-deep"), DEEP_TO_SUPERFICIAL("deep-to-superficial"),
		APICAL_TO_BASAL("apical-to-basal"), BASAL_TO_APICAL("basal-to-apical"),
		APEX_TO_BASE("apex-to-base"), BASE_TO_APEX("base-to-apex");

		private final String value;

		Anatomical(final String value) {

			this.value = value;
		}

		public String getValue() {

			return value;
		}

		/**
		 * @param value a string representation, e.g. "left-to-right"
		 * @return the matching value, or null if none matches
		 */
		public static Anatomical fromString(final String value) {

			for (final Anatomical v : values())
				if (v.value.equals(value))
					return v;

			return null;
		}

		@Override
		public String toString() {

			return value;
		}
	}

	private String type;
	private String value;

	public Orientation(String type, String value) {

		this.type = type;
		this.value = value;
	}

	public Orientation(Anatomical value) {

		this(ANATOMICAL, value.getValue());
	}

	public String getValue() {

		return value;
	}

	public void setValue(String value) {

		this.value = value;
	}

	public String getType() {

		return type;
	}

	public void setType(String type) {

		this.type = type;
	}

	/**
	 * @return true if the type is "anatomical" and the value is one of
	 *         {@link Anatomical}
	 */
	public boolean isValid() {

		return ANATOMICAL.equals(type) && Anatomical.fromString(value) != null;
	}

	public Orientation copy() {

		return new Orientation(type, value);
	}

	@Override
	public boolean equals(final Object other) {

		if (other instanceof Orientation) {
			final Orientation orientation = (Orientation) other;
			return Objects.equals(type, orientation.type) && Objects.equals(value, orientation.value);
		}
		return false;
	}

	@Override
	public int hashCode() {

		return Objects.hash(type, value);
	}

	@Override
	public String toString() {

		return String.format("%s: %s", type, value);
	}

}
