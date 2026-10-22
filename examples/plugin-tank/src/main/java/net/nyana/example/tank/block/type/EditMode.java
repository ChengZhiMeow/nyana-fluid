package net.nyana.example.tank.block.type;

public enum EditMode {
    BOTH("both", "双向", "满的容器倒进储罐, 空的从储罐装"),
    FILL("fill", "装入物品", "只从储罐装进容器"),
    EMPTY("empty", "倒入储罐", "只把容器里的倒进储罐");

    private final String icon;
    private final String label;
    private final String description;

    EditMode(String icon, String label, String description) {
        this.icon = icon;
        this.label = label;
        this.description = description;
    }

    public String icon() {
        return this.icon;
    }

    public String label() {
        return this.label;
    }

    public String description() {
        return this.description;
    }

    public EditMode next() {
        return EditMode.values()[(super.ordinal() + 1) % EditMode.values().length];
    }
}
