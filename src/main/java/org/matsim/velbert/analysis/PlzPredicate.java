package org.matsim.velbert.analysis;

import org.locationtech.jts.geom.Geometry;
import org.locationtech.jts.geom.Point;
import org.matsim.api.core.v01.Coord;
import org.matsim.core.utils.geometry.CoordinateTransformation;
import org.matsim.core.utils.geometry.geotools.MGC;
import org.matsim.core.utils.geometry.transformations.TransformationFactory;
import org.opengis.feature.simple.SimpleFeature;

import java.util.Collection;
import java.util.List;
import java.util.function.Predicate;
import java.util.stream.Collectors;

public class PlzPredicate implements Predicate<Coord> {

	private static final CoordinateTransformation transformation = TransformationFactory.getCoordinateTransformation("EPSG:25832", "EPSG:3857");
	private final List<Geometry> geometries;

	public PlzPredicate(Collection<SimpleFeature> allFeatures, List<String> plzList) {
		geometries = allFeatures.stream()
				.filter(feature -> plzList.contains(feature.getAttribute("plz")))
				.map(feature -> (Geometry) feature.getDefaultGeometry())
				.collect(Collectors.toList());
	}

	@Override
	public boolean test(Coord coord) {
		Point point = MGC.coord2Point(transformation.transform(coord));
		return geometries.stream().anyMatch(plz -> plz.covers(point));
	}
}
