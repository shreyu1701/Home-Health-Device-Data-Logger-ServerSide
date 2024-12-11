module com.zodiac.homehealthdevicedataloggerserver {
	requires javafx.controls;
	requires javafx.fxml;
	requires javafx.web;

	requires org.controlsfx.controls;
	requires com.dlsc.formsfx;
	requires net.synedra.validatorfx;
	requires org.kordamp.ikonli.javafx;
	requires org.kordamp.bootstrapfx.core;
	requires eu.hansolo.tilesfx;
	requires com.almasb.fxgl.all;
	requires org.apache.commons.net;
    requires java.sql;

    opens com.zodiac.homehealthdevicedataloggerserver to javafx.fxml;
	exports com.zodiac.homehealthdevicedataloggerserver;
}