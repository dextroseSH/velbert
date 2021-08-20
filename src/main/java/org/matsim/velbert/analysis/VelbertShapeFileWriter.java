package org.matsim.velbert.analysis;

import org.geotools.geometry.jts.JTS;
import org.geotools.referencing.CRS;
import org.locationtech.jts.geom.Geometry;
import org.locationtech.jts.geom.prep.PreparedGeometryFactory;
import org.matsim.core.utils.gis.ShapeFileReader;
import org.matsim.core.utils.gis.ShapeFileWriter;
import org.opengis.referencing.FactoryException;
import org.opengis.referencing.operation.MathTransform;
import org.opengis.referencing.operation.TransformException;

import java.net.MalformedURLException;
import java.net.URI;
import java.util.Set;
import java.util.stream.Collectors;

public class VelbertShapeFileWriter {

    private static final String shapeFile = "https://svn.vsp.tu-berlin.de/repos/public-svn/matsim/scenarios/countries/de/velbert/velbert-v1.0/shapes/Postleitzahlengebiete-shp/OSM_PLZ_072019.shp";
    private static final Set<String> zipCodes = Set.of("42551", "42549", "42555", "42553");

    public static void main(String[] args) throws FactoryException, MalformedURLException {
        var factory = new PreparedGeometryFactory();
        var fromCRS = CRS.decode("EPSG:3857");
        var toCRS = CRS.decode("EPSG:25832");
        var transformation = CRS.findMathTransform(fromCRS, toCRS);

        var uri = URI.create(shapeFile);
        var s = ShapeFileReader.getAllFeatures(uri.toURL()).stream()
                .filter(simpleFeature -> zipCodes.contains((String) simpleFeature.getAttribute("plz")))
                .collect(Collectors.toSet());

        ShapeFileWriter.writeGeometries(s, "scenarios/equil/drt/velbert.shp");
    }

    private static Geometry transform(Geometry geometry, MathTransform transform) {
        try {
            return JTS.transform(geometry, transform);
        } catch (TransformException e) {
            throw new RuntimeException(e);
        }
    }
}
