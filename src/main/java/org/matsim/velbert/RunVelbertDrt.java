package org.matsim.velbert;

import org.geotools.geometry.jts.JTS;
import org.geotools.referencing.CRS;
import org.locationtech.jts.geom.Geometry;
import org.locationtech.jts.geom.prep.PreparedGeometry;
import org.locationtech.jts.geom.prep.PreparedGeometryFactory;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.contrib.drt.optimizer.insertion.ExtensiveInsertionSearchParams;
import org.matsim.contrib.drt.routing.DrtRoute;
import org.matsim.contrib.drt.routing.DrtRouteFactory;
import org.matsim.contrib.drt.run.DrtConfigGroup;
import org.matsim.contrib.drt.run.DrtConfigs;
import org.matsim.contrib.drt.run.MultiModeDrtConfigGroup;
import org.matsim.contrib.drt.run.MultiModeDrtModule;
import org.matsim.contrib.dvrp.run.DvrpConfigGroup;
import org.matsim.contrib.dvrp.run.DvrpModule;
import org.matsim.contrib.dvrp.run.DvrpQSimComponents;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.config.groups.PlanCalcScoreConfigGroup;
import org.matsim.core.config.groups.PlansCalcRouteConfigGroup;
import org.matsim.core.config.groups.QSimConfigGroup;
import org.matsim.core.config.groups.StrategyConfigGroup;
import org.matsim.core.controler.AbstractModule;
import org.matsim.core.controler.Controler;
import org.matsim.core.replanning.strategies.DefaultPlanStrategiesModule;
import org.matsim.core.router.TripStructureUtils;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.utils.geometry.geotools.MGC;
import org.matsim.core.utils.gis.ShapeFileReader;
import org.matsim.velbert.analysis.TripAnalyzerModule;
import org.opengis.referencing.FactoryException;
import org.opengis.referencing.operation.MathTransform;
import org.opengis.referencing.operation.TransformException;

import java.net.MalformedURLException;
import java.net.URI;
import java.util.Collection;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

public class RunVelbertDrt {

    private static final String shapeFile = "https://svn.vsp.tu-berlin.de/repos/public-svn/matsim/scenarios/countries/de/velbert/velbert-v1.0/shapes/Postleitzahlengebiete-shp/OSM_PLZ_072019.shp";
    private static final Set<String> zipCodes = Set.of("42551", "42549", "42555", "42553");
    private static final String DRT_A = "drt_A";

    public static void main(String[] args) throws MalformedURLException, FactoryException {

        var config = ConfigUtils.loadConfig(args);
        config.controler().setLastIteration(50);

        config.plansCalcRoute().setAccessEgressType(PlansCalcRouteConfigGroup.AccessEgressType.accessEgressModeToLink);

        for (long ii = 600; ii <= 97200; ii += 600) {

            for (String act : List.of("educ_higher", "educ_kiga", "educ_other", "educ_primary", "educ_secondary",
                    "educ_tertiary", "errands", "home", "visit")) {
                config.planCalcScore()
                        .addActivityParams(new PlanCalcScoreConfigGroup.ActivityParams(act + "_" + ii + ".0").setTypicalDuration(ii));
            }

            config.planCalcScore().addActivityParams(new PlanCalcScoreConfigGroup.ActivityParams("work_" + ii + ".0").setTypicalDuration(ii)
                    .setOpeningTime(6. * 3600.).setClosingTime(20. * 3600.));
            config.planCalcScore().addActivityParams(new PlanCalcScoreConfigGroup.ActivityParams("business_" + ii + ".0").setTypicalDuration(ii)
                    .setOpeningTime(6. * 3600.).setClosingTime(20. * 3600.));
            config.planCalcScore().addActivityParams(new PlanCalcScoreConfigGroup.ActivityParams("leisure_" + ii + ".0").setTypicalDuration(ii)
                    .setOpeningTime(9. * 3600.).setClosingTime(27. * 3600.));
            config.planCalcScore().addActivityParams(new PlanCalcScoreConfigGroup.ActivityParams("shop_daily_" + ii + ".0").setTypicalDuration(ii)
                    .setOpeningTime(8. * 3600.).setClosingTime(20. * 3600.));
            config.planCalcScore().addActivityParams(new PlanCalcScoreConfigGroup.ActivityParams("shop_other_" + ii + ".0").setTypicalDuration(ii)
                    .setOpeningTime(8. * 3600.).setClosingTime(20. * 3600.));
        }

        config.qsim().setSimStarttimeInterpretation( QSimConfigGroup.StarttimeInterpretation.onlyUseStarttime );
        config.qsim().setInsertingWaitingVehiclesBeforeDrivingVehicles(true);
        config.qsim().setSnapshotStyle(QSimConfigGroup.SnapshotStyle.queue);

        @SuppressWarnings("unused")
        DvrpConfigGroup dvrpConfig = ConfigUtils.addOrGetModule( config, DvrpConfigGroup.class );

        MultiModeDrtConfigGroup multiModeDrtCfg = ConfigUtils.addOrGetModule(config, MultiModeDrtConfigGroup.class);

        {
            DrtConfigGroup drtConfig = new DrtConfigGroup();
            drtConfig.setMode( DRT_A ).setStopDuration(60.).setMaxWaitTime(900.).setMaxTravelTimeAlpha(1.3).setMaxTravelTimeBeta(10. * 60.);
            drtConfig.setRejectRequestIfMaxWaitOrTravelTimeViolated( false );
            drtConfig.setVehiclesFile("drtVehicles.xml");
            drtConfig.setChangeStartLinkToLastLinkInSchedule(true);
            drtConfig.addParameterSet( new ExtensiveInsertionSearchParams() );
            drtConfig.setDrtServiceAreaShapeFile("shape/velbert.shp");
            drtConfig.setOperationalScheme(DrtConfigGroup.OperationalScheme.serviceAreaBased);
            multiModeDrtCfg.addDrtConfig(drtConfig);
        }

        for (DrtConfigGroup drtCfg : multiModeDrtCfg.getModalElements()) {
            DrtConfigs.adjustDrtConfig(drtCfg, config.planCalcScore(), config.plansCalcRoute());
        }

        config.strategy().clearStrategySettings();
        config.strategy().addStrategySettings(new StrategyConfigGroup.StrategySettings().setStrategyName(DefaultPlanStrategiesModule.DefaultStrategy.ChangeSingleTripMode).setWeight(0.15).setSubpopulation("person"));
        config.changeMode().setModes(new String[]{TransportMode.ride, TransportMode.bike, TransportMode.car, TransportMode.pt, DRT_A});
        config.changeMode().setIgnoreCarAvailability(false);

        config.strategy().addStrategySettings(new StrategyConfigGroup.StrategySettings().setStrategyName(DefaultPlanStrategiesModule.DefaultStrategy.TimeAllocationMutator).setWeight(0.05).setSubpopulation("person"));
        config.timeAllocationMutator().setMutationRange(7200.0);
        config.timeAllocationMutator().setAffectingDuration(false);

        config.strategy().addStrategySettings(new StrategyConfigGroup.StrategySettings().setStrategyName(DefaultPlanStrategiesModule.DefaultStrategy.ReRoute).setWeight(0.1).setSubpopulation("person"));

        config.strategy().addStrategySettings(new StrategyConfigGroup.StrategySettings().setStrategyName(DefaultPlanStrategiesModule.DefaultSelector.ChangeExpBeta).setWeight(0.7).setSubpopulation("person"));

        config.planCalcScore().addModeParams(new PlanCalcScoreConfigGroup.ModeParams(DRT_A));

        Scenario scenario = ScenarioUtils.createScenario( config ) ;
        scenario.getPopulation().getFactory().getRouteFactories().setRouteFactory( DrtRoute.class, new DrtRouteFactory() );
        ScenarioUtils.loadScenario( scenario );

        var controler = new Controler(scenario);

        controler.addOverridingModule( new DvrpModule() ) ;
        controler.addOverridingModule( new MultiModeDrtModule( ) ) ;

        controler.configureQSimComponents( DvrpQSimComponents.activateModes( DRT_A ) ) ;

        // use the (congested) car travel time for the teleported ride mode
        controler.addOverridingModule(new AbstractModule() {
            @Override
            public void install() {
                addTravelTimeBinding(TransportMode.ride).to(networkTravelTime());
                addTravelDisutilityFactoryBinding(TransportMode.ride).to(carTravelDisutilityFactoryKey());
            }
        });

        controler.run();
    }

    private static Collection<PreparedGeometry> getDilutionArea() throws FactoryException, MalformedURLException {

        var factory = new PreparedGeometryFactory();
        var fromCRS = CRS.decode("EPSG:3857");
        var toCRS = CRS.decode("EPSG:25832");
        var transformation = CRS.findMathTransform(fromCRS, toCRS);

        var uri = URI.create(shapeFile);
        return ShapeFileReader.getAllFeatures(uri.toURL()).stream()
                .filter(simpleFeature -> zipCodes.contains((String) simpleFeature.getAttribute("plz")))
                .map(simpleFeature -> (Geometry) simpleFeature.getDefaultGeometry())
                .map(geometry -> transform(geometry, transformation))
                .map(factory::create)
                .collect(Collectors.toSet());

    }

    private static Geometry transform(Geometry geometry, MathTransform transform) {
        try {
            return JTS.transform(geometry, transform);
        } catch (TransformException e) {
            throw new RuntimeException(e);
        }
    }
}
