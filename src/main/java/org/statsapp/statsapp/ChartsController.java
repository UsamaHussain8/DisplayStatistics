package org.statsapp.statsapp;

import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.chart.PieChart;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.scene.layout.VBox;

import java.io.IOException;
import java.util.Map;

public class ChartsController extends VBox {

    @FXML
    private PieChart nationalitiesChart;

    @FXML
    private PieChart rolesChart;

    @FXML
    private PieChart yearsChart;

    public ChartsController() {
        FXMLLoader fxmlLoader = new FXMLLoader(getClass().getResource("ChartsWindow.fxml"));
        fxmlLoader.setRoot(this);
        fxmlLoader.setController(this);

        nationalitiesChart = new PieChart();
        rolesChart = new PieChart();
        yearsChart = new PieChart();

        try {
            fxmlLoader.load();
        }
        catch (IOException exception) {
            throw new RuntimeException(exception);
        }
    }

    public void setChartData(Map<String, Integer> nationalitiesData,
                             Map<String, Integer> rolesData,
                             Map<String, Integer> yearsData) {

        System.out.println(nationalitiesData.size());
        // Populate Nationalities Pie Chart
        nationalitiesChart.setData(convertToChartData(nationalitiesData));

        // Populate Roles Pie Chart
        rolesChart.setData(convertToChartData(rolesData));

        // Populate Years Pie Chart
        yearsChart.setData(convertToChartData(yearsData));
    }

//    private ObservableList<PieChart.Data> convertToChartData(Map<String, Integer> data) {
//        ObservableList<PieChart.Data> chartData = FXCollections.observableArrayList();
//        data.forEach((key, value) -> chartData.add(new PieChart.Data(key, value)));
//        return chartData;
//    }
    private ObservableList<PieChart.Data> convertToChartData(Map<String, Integer> data) {
        ObservableList<PieChart.Data> chartData = FXCollections.observableArrayList();

        // Calculate the total count to compute proportions
        int total = data.values().stream().mapToInt(Integer::intValue).sum();

        // Add data with proportions to the chart
        data.forEach((key, value) -> {
            double percentage = (value * 100.0) / total;
            String label = String.format("%s (%.1f%%)", key, percentage); // Include proportion in label
            chartData.add(new PieChart.Data(label, value));
        });

        return chartData;
    }

}
