package team4618.dashboard.pages;

import javafx.scene.Node;

import java.util.ArrayList;

public abstract class DashboardPage {
    public static ArrayList<DashboardPage> pages = new ArrayList<>();

    public DashboardPage() {
        pages.add(this);
    }

    public abstract void setPageSelected(boolean selected);
    public abstract Node getNode();

    public static void setSelectedPage(DashboardPage page) {
        pages.forEach(p -> p.setPageSelected(page == p));
        page.setPageSelected(true);
    }
}
