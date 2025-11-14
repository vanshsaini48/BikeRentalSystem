import java.awt.*;
import java.awt.event.*;
import java.awt.geom.*;
import java.awt.image.BufferedImage;
import java.io.*;
import java.nio.file.*;
import java.time.LocalDate;
import java.util.*;
import javax.swing.*;
import javax.swing.border.EmptyBorder;

public class BikeRentalSystem extends JFrame {
    private Bike[] bikes = new Bike[50];
    private Customer[] customers = new Customer[50];
    private Rental[] rentals = new Rental[200];

    private int bikeCount = 0;
    private int custCount = 0;
    private int rentCount = 0;

    private final RentalPolicy rentalPolicy = new SimplePolicy();

    private static final String BIKES_FILE = "bikes.txt";
    private static final String CUSTOMERS_FILE = "customers.txt";
    private static final String RENTALS_FILE = "rentals.txt";

    private JPanel root;

    public BikeRentalSystem() {
        super("Bike Rental System — Kashmir");
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setSize(820, 560);
        setLocationRelativeTo(null);
        setIconImage(createAppIcon());
        installUIFont();

        loadAll();

        root = new ShawlWatermarkPanel();
        root.setLayout(new BorderLayout());
        root.setBorder(new EmptyBorder(12, 12, 12, 12));
        setContentPane(root);

        JPanel header = new MountainsBannerPanel();
        header.setPreferredSize(new Dimension(800, 140));
        header.setLayout(new BorderLayout());
        JLabel title = new JLabel("Kashmir Bike Rentals", SwingConstants.LEFT);
        title.setForeground(new Color(255, 255, 255));
        title.setBorder(new EmptyBorder(16, 20, 0, 0));
        title.setFont(title.getFont().deriveFont(Font.BOLD, 26f));
        JLabel subtitle = new JLabel("Simple • Offline • Local", SwingConstants.LEFT);
        subtitle.setForeground(new Color(240, 240, 240));
        subtitle.setBorder(new EmptyBorder(0, 22, 16, 0));
        subtitle.setFont(subtitle.getFont().deriveFont(Font.PLAIN, 14f));
        JPanel titleBox = new JPanel(new GridLayout(2,1));
        titleBox.setOpaque(false);
        titleBox.add(title);
        titleBox.add(subtitle);
        header.add(titleBox, BorderLayout.WEST);
        root.add(header, BorderLayout.NORTH);

        JPanel centerCard = new JPanel();
        centerCard.setOpaque(false);
        centerCard.setLayout(new GridBagLayout());
        JPanel buttonGrid = new RoundedCardPanel();
        buttonGrid.setLayout(new GridLayout(2, 3, 14, 14));
        buttonGrid.setBorder(new EmptyBorder(18, 18, 18, 18));

        BikeIcon bikeIcon = new BikeIcon(24, 24);

        JButton registerBtn = new RoundedButton("Register Customer", bikeIcon);
        JButton showBikesBtn = new RoundedButton("View Available Bikes", bikeIcon);
        JButton rentBtn = new RoundedButton("Rent a Bike", bikeIcon);
        JButton returnBtn = new RoundedButton("Return Bike", bikeIcon);
        JButton recordsBtn = new RoundedButton("View Records", bikeIcon);

        buttonGrid.add(registerBtn);
        buttonGrid.add(showBikesBtn);
        buttonGrid.add(rentBtn);
        buttonGrid.add(returnBtn);
        buttonGrid.add(recordsBtn);

        GridBagConstraints gbc = new GridBagConstraints();
        gbc.gridx = 0; gbc.gridy = 0; gbc.weightx = 1; gbc.weighty = 1; gbc.fill = GridBagConstraints.NONE;
        centerCard.add(buttonGrid, gbc);
        root.add(centerCard, BorderLayout.CENTER);

        JLabel footer = new JLabel("Data auto-saves after every action", SwingConstants.CENTER);
        footer.setBorder(new EmptyBorder(6, 0, 0, 0));
        root.add(footer, BorderLayout.SOUTH);

        registerBtn.addActionListener(e -> registerCustomer());
        showBikesBtn.addActionListener(e -> showBikes());
        rentBtn.addActionListener(e -> rentBike());
        returnBtn.addActionListener(e -> returnBike());
        recordsBtn.addActionListener(e -> viewRecords());
    }

    private void loadAll() {
        try {
            loadBikes();
            loadCustomers();
            loadRentals();
        } catch (Exception ignore) {}

        if (bikeCount == 0) {
            loadDefaultBikes();
            saveBikes();
        }
    }

    private void loadBikes() {
        bikeCount = 0;
        Path p = Paths.get(BIKES_FILE);
        if (!Files.exists(p)) return;
        try (BufferedReader br = Files.newBufferedReader(p)) {
            String line;
            while ((line = br.readLine()) != null) {
                if (line.trim().isEmpty() || line.startsWith("#")) continue;
                String[] parts = line.split("\\|");
                if (parts.length >= 5) {
                    String id = parts[0];
                    String model = parts[1];
                    int rate = Integer.parseInt(parts[2]);
                    boolean available = Boolean.parseBoolean(parts[3]);
                    Bike b = new Bike(id, model, rate);
                    b.setAvailable(available);
                    bikes[bikeCount++] = b;
                }
            }
        } catch (Exception ignored) {}
    }

    private void saveBikes() {
        try (PrintWriter pw = new PrintWriter(Files.newBufferedWriter(Paths.get(BIKES_FILE)))) {
            pw.println("# id|model|rate|available");
            for (int i = 0; i < bikeCount; i++) {
                Bike b = bikes[i];
                pw.printf(Locale.ROOT, "%s|%s|%d|%b%n", b.getBikeId(), b.getModel(), b.getRate(), b.isAvailable());
            }
        } catch (IOException ignored) {}
    }

    private void loadCustomers() {
        custCount = 0;
        Path p = Paths.get(CUSTOMERS_FILE);
        if (!Files.exists(p)) return;
        try (BufferedReader br = Files.newBufferedReader(p)) {
            String line;
            while ((line = br.readLine()) != null) {
                if (line.trim().isEmpty() || line.startsWith("#")) continue;
                String[] parts = line.split("\\|");
                if (parts.length >= 4) {
                    String id = parts[0];
                    String name = parts[1];
                    int age = Integer.parseInt(parts[2]);
                    String lic = parts[3];
                    customers[custCount++] = new Customer(id, name, age, lic);
                }
            }
        } catch (Exception ignored) {}
    }

    private void saveCustomers() {
        try (PrintWriter pw = new PrintWriter(Files.newBufferedWriter(Paths.get(CUSTOMERS_FILE)))) {
            pw.println("# id|name|age|license");
            for (int i = 0; i < custCount; i++) {
                Customer c = customers[i];
                pw.printf(Locale.ROOT, "%s|%s|%d|%s%n", c.getId(), escape(c.getName()), c.getAge(), c.getLicense());
            }
        } catch (IOException ignored) {}
    }

    private void loadRentals() {
        rentCount = 0;
        Path p = Paths.get(RENTALS_FILE);
        if (!Files.exists(p)) return;
        try (BufferedReader br = Files.newBufferedReader(p)) {
            String line;
            while ((line = br.readLine()) != null) {
                if (line.trim().isEmpty() || line.startsWith("#")) continue;
                String[] parts = line.split("\\|");
                if (parts.length >= 10) {
                    String rid = parts[0];
                    String bid = parts[1];
                    String cid = parts[2];
                    int days = Integer.parseInt(parts[3]);
                    int baseRent = Integer.parseInt(parts[4]);
                    int damageFee = Integer.parseInt(parts[5]);
                    int total = Integer.parseInt(parts[6]);
                    boolean returned = Boolean.parseBoolean(parts[7]);
                    String startDate = parts[8];
                    String returnDate = parts[9];
                    Rental r = new Rental(rid, bid, cid, days, baseRent);
                    r.damageFee = damageFee;
                    r.total = total;
                    r.returned = returned;
                    r.startDate = startDate;
                    r.returnDate = returnDate;
                    rentals[rentCount++] = r;
                }
            }
        } catch (Exception ignored) {}
    }

    private void saveRentals() {
        try (PrintWriter pw = new PrintWriter(Files.newBufferedWriter(Paths.get(RENTALS_FILE)))) {
            pw.println("# rid|bikeId|customerId|days|baseRent|damageFee|total|returned|startDate|returnDate");
            for (int i = 0; i < rentCount; i++) {
                Rental r = rentals[i];
                pw.printf(Locale.ROOT, "%s|%s|%s|%d|%d|%d|%d|%b|%s|%s%n",
                        r.rentalId, r.bikeId, r.customerId, r.days, r.baseRent, r.damageFee,
                        r.total, r.returned, r.startDate, r.returnDate);
            }
        } catch (IOException ignored) {}
    }

    private static String escape(String v) {
        return v.replace("|", "/");
    }

    private void loadDefaultBikes() {
        bikes[bikeCount++] = new Bike("B101", "Royal Enfield Classic 350", 900);
        bikes[bikeCount++] = new Bike("B102", "Pulsar 180", 700);
        bikes[bikeCount++] = new Bike("B103", "Apache RTR", 650);
    }

    private void registerCustomer() {
        JTextField nameF = new JTextField();
        JTextField ageF = new JTextField();
        JTextField licF = new JTextField();
        Object[] msg = {"Name:", nameF, "Age:", ageF, "License No:", licF};
        int ok = JOptionPane.showConfirmDialog(this, msg, "Register Customer", JOptionPane.OK_CANCEL_OPTION);
        if (ok != JOptionPane.OK_OPTION) return;
        try {
            String name = nameF.getText().trim();
            int age = Integer.parseInt(ageF.getText().trim());
            String license = licF.getText().trim();
            Customer newCust = new Customer("C" + (custCount + 1), name, age, license);
            if (!rentalPolicy.eligible(newCust)) {
                throw new InvalidLicenseException("Age should be at least 18 and license must be valid.");
            }
            customers[custCount++] = newCust;
            saveCustomers();
            JOptionPane.showMessageDialog(this, "Customer Registered:\n" + newCust.basicInfo());
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this, ex.getMessage());
        }
    }

    private void showBikes() {
        StringBuilder sb = new StringBuilder("Available Bikes:\n\n");
        for (int i = 0; i < bikeCount; i++) sb.append(bikes[i]).append("\n");
        JTextArea area = new JTextArea(sb.toString(), 14, 40);
        area.setEditable(false);
        area.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 13));
        JScrollPane sp = new JScrollPane(area);
        sp.setPreferredSize(new Dimension(540, 320));
        JOptionPane.showMessageDialog(this, sp, "Bikes", JOptionPane.PLAIN_MESSAGE);
    }

    private void rentBike() {
        String license = JOptionPane.showInputDialog(this, "Enter Customer License:");
        Customer c = findCustomer(license);
        if (c == null) { JOptionPane.showMessageDialog(this, "Customer not found."); return; }
        String bikeId = JOptionPane.showInputDialog(this, "Enter Bike ID:");
        Bike b = findBike(bikeId);
        if (b == null || !b.isAvailable()) { JOptionPane.showMessageDialog(this, "Bike not available."); return; }
        int days = Integer.parseInt(JOptionPane.showInputDialog(this, "Days for rent:"));
        int baseCost = b.getRate() * days;
        Rental r = new Rental("R" + (rentCount + 1), bikeId, c.getId(), days, baseCost);
        rentals[rentCount++] = r;
        b.setAvailable(false);
        saveBikes();
        saveRentals();
        JOptionPane.showMessageDialog(this, "Bike Rented\nRental ID: " + r.rentalId + "\nTotal Cost: ₹" + baseCost);
    }

    private void returnBike() {
        String rid = JOptionPane.showInputDialog(this, "Enter Rental ID:");
        Rental r = findRental(rid);
        if (r == null || r.returned) { JOptionPane.showMessageDialog(this, "Invalid Rental."); return; }
        String[] damageOptions = {"No Damage", "Minor Scratch", "Moderate Damage", "Heavy Damage"};
        String damage = (String) JOptionPane.showInputDialog(this, "Select Damage Level", "Return",
                JOptionPane.PLAIN_MESSAGE, null, damageOptions, damageOptions[0]);
        int extra = rentalPolicy.damageFee(damage);
        r.damageFee = extra; r.total = r.baseRent + extra; r.returnDate = LocalDate.now().toString(); r.returned = true;
        Bike b = findBike(r.bikeId); if (b != null) b.setAvailable(true);
        saveBikes(); saveRentals();
        JOptionPane.showMessageDialog(this, "Return Complete\nTotal Payable: ₹" + r.total);
    }

    private void viewRecords() {
        StringBuilder sb = new StringBuilder("Rental History:\n\n");
        for (int i = 0; i < rentCount; i++) {
            Rental r = rentals[i];
            sb.append(r.rentalId).append(" | ").append(r.bikeId).append(" | ").append(r.customerId)
              .append(" | Total: ₹").append(r.total).append(r.returned ? " (Returned)" : " (Ongoing)")
              .append(" | Start: ").append(r.startDate).append(" | Return: ").append(r.returnDate).append("\n");
        }
        JTextArea area = new JTextArea(sb.toString(), 16, 50);
        area.setEditable(false);
        area.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 13));
        JScrollPane sp = new JScrollPane(area);
        sp.setPreferredSize(new Dimension(640, 360));
        JOptionPane.showMessageDialog(this, sp, "Records", JOptionPane.PLAIN_MESSAGE);
    }

    private Customer findCustomer(String license) {
        if (license == null) return null;
        license = license.trim().toLowerCase();
        for (int i = 0; i < custCount; i++) {
            if (customers[i].getLicense().trim().toLowerCase().equals(license)) return customers[i];
        }
        return null;
    }

    private Bike findBike(String id) {
        for (int i = 0; i < bikeCount; i++) if (bikes[i].getBikeId().equals(id)) return bikes[i];
        return null;
    }

    private Rental findRental(String rid) {
        for (int i = 0; i < rentCount; i++) if (rentals[i].rentalId.equals(rid)) return rentals[i];
        return null;
    }

    private static void installUIFont() {
        String[] prefs = {"Segoe UI", "Inter", "Roboto", "Helvetica Neue", "Noto Sans", "Arial"};
        Font base = UIManager.getFont("Label.font");
        Font f = base;
        for (String p : prefs) {
            Font test = new Font(p, Font.PLAIN, 13);
            if (test.getFamily().equals(p)) { f = test; break; }
        }
        Enumeration<?> keys = UIManager.getDefaults().keys();
        while (keys.hasMoreElements()) {
            Object key = keys.nextElement();
            Object val = UIManager.get(key);
            if (val instanceof Font) UIManager.put(key, f);
        }
    }

    private static Image createAppIcon() {
        int s = 64;
        BufferedImage img = new BufferedImage(s, s, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = img.createGraphics();
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g.setColor(new Color(30, 144, 255));
        g.fillRoundRect(0, 0, s, s, 18, 18);
        new BikeIcon(42, 42).paintIcon(null, g, 11, 11);
        g.dispose();
        return img;
    }

    static class ShawlWatermarkPanel extends JPanel {
        @Override protected void paintComponent(Graphics gg) {
            super.paintComponent(gg);
            Graphics2D g = (Graphics2D) gg;
            g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g.setColor(new Color(250, 250, 248));
            g.fillRect(0, 0, getWidth(), getHeight());
            Shape paisley = createPaisleyShape();
            int step = 120;
            for (int y = 0; y < getHeight() + step; y += step) {
                for (int x = 0; x < getWidth() + step; x += step) {
                    AffineTransform at = new AffineTransform();
                    at.translate(x + 20, y + 10);
                    at.rotate(Math.toRadians((x/step + y/step) % 2 == 0 ? 15 : -12));
                    at.scale(0.8, 0.8);
                    Shape s = at.createTransformedShape(paisley);
                    g.setColor(new Color(200, 64, 64, 22));
                    g.fill(s);
                    g.setStroke(new BasicStroke(1f));
                    g.setColor(new Color(150, 42, 42, 28));
                    g.draw(s);
                }
            }
        }
        private static Shape createPaisleyShape() {
            GeneralPath p = new GeneralPath();
            p.moveTo(0, 0);
            p.quadTo(30, -20, 58, 8);
            p.quadTo(88, 36, 60, 64);
            p.quadTo(36, 86, 14, 60);
            p.quadTo(-6, 38, 0, 0);
            p.closePath();
            GeneralPath inner = new GeneralPath();
            inner.moveTo(22, 20);
            inner.quadTo(42, 6, 52, 22);
            inner.quadTo(60, 36, 44, 44);
            inner.quadTo(30, 50, 22, 36);
            inner.closePath();
            Area a = new Area(p); a.subtract(new Area(inner));
            return a;
        }
    }

    static class MountainsBannerPanel extends JPanel {
        @Override protected void paintComponent(Graphics gg) {
            super.paintComponent(gg);
            Graphics2D g = (Graphics2D) gg;
            g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            int w = getWidth(), h = getHeight();
            GradientPaint sky = new GradientPaint(0, 0, new Color(44, 62, 80), 0, h, new Color(88, 130, 193));
            g.setPaint(sky); g.fillRect(0, 0, w, h);
            g.setColor(new Color(255, 255, 255, 60));
            g.fillOval(w - 180, 20, 120, 120);
            drawMountain(g, w, h, 0.65, new Color(20, 30, 50, 170));
            drawMountain(g, w, h, 0.75, new Color(30, 44, 70, 140));
            drawMountain(g, w, h, 0.85, new Color(40, 60, 90, 120));
            g.setPaint(new GradientPaint(0, (int)(h*0.55), new Color(255,255,255,90), 0, h, new Color(255,255,255,0)));
            g.fillRect(0, (int)(h*0.45), w, (int)(h*0.55));
        }
        private void drawMountain(Graphics2D g, int w, int h, double scaleY, Color c) {
            int base = (int)(h * scaleY);
            Path2D p = new Path2D.Double();
            p.moveTo(0, base);
            p.curveTo(w*0.15, base-60, w*0.25, base-80, w*0.35, base-20);
            p.curveTo(w*0.45, base+10, w*0.60, base-70, w*0.70, base-30);
            p.curveTo(w*0.82, base+10, w*0.90, base-50, w, base-10);
            p.lineTo(w, h);
            p.lineTo(0, h);
            p.closePath();
            g.setColor(c);
            g.fill(p);
        }
    }

    static class RoundedCardPanel extends JPanel {
        public RoundedCardPanel() { setOpaque(false); }
        @Override protected void paintComponent(Graphics gg) {
            Graphics2D g = (Graphics2D) gg;
            g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            int w = getWidth(), h = getHeight();
            g.setColor(new Color(0,0,0,30));
            g.fillRoundRect(8, 10, w-8-8, h-10-8, 22, 22);
            g.setColor(new Color(255, 255, 255, 235));
            g.fillRoundRect(0, 0, w-8, h-8, 22, 22);
            super.paintComponent(gg);
        }
    }

    static class RoundedButton extends JButton {
        private boolean hover = false;
        public RoundedButton(String text, Icon icon) {
            super(text, icon);
            setFocusPainted(false);
            setContentAreaFilled(false);
            setBorderPainted(false);
            setOpaque(false);
            setHorizontalAlignment(SwingConstants.LEFT);
            setIconTextGap(10);
            setMargin(new Insets(12, 16, 12, 16));
            setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
            addMouseListener(new MouseAdapter() {
                @Override public void mouseEntered(MouseEvent e) { hover = true; repaint(); }
                @Override public void mouseExited(MouseEvent e) { hover = false; repaint(); }
            });
        }
        @Override protected void paintComponent(Graphics gg) {
            Graphics2D g = (Graphics2D) gg;
            g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            int w = getWidth(), h = getHeight();
            g.setColor(new Color(0,0,0, hover ? 50 : 35));
            g.fillRoundRect(4, 6, w-4-4, h-6-4, 18, 18);
            GradientPaint gp = new GradientPaint(0, 0, hover ? new Color(64, 145, 255) : new Color(52, 120, 220),
                                                 0, h, hover ? new Color(44, 110, 230) : new Color(40, 98, 190));
            g.setPaint(gp);
            g.fillRoundRect(0, 0, w-6, h-6, 18, 18);
            if (isFocusOwner()) {
                g.setStroke(new BasicStroke(2f));
                g.setColor(new Color(255, 255, 255, 160));
                g.drawRoundRect(2, 2, w-10, h-10, 16, 16);
            }
            g.setColor(Color.WHITE);
            FontMetrics fm = g.getFontMetrics();
            int textX = 18 + 26 + 10;
            int textY = (h + fm.getAscent() - fm.getDescent()) / 2 - 3;
            if (getIcon() != null) getIcon().paintIcon(this, g, 18, (h - getIcon().getIconHeight())/2 - 2);
            g.drawString(getText(), textX, textY);
        }
    }

    static class BikeIcon implements Icon {
        private final int w, h;
        public BikeIcon(int w, int h) { this.w = w; this.h = h; }
        @Override public void paintIcon(Component c, Graphics gg, int x, int y) {
            Graphics2D g = (Graphics2D) gg.create();
            g.translate(x, y);
            g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g.setStroke(new BasicStroke(2f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
            g.setColor(new Color(255,255,255));
            g.drawOval(0, h-12, 12, 12);
            g.drawOval(w-14, h-12, 12, 12);
            g.drawLine(6, h-6, w/2-2, h-16);
            g.drawLine(w/2-2, h-16, w-8, h-6);
            g.drawLine(w/2-2, h-16, w/2-6, h-6);
            g.drawLine(w-10, h-16, w-4, h-18);
            g.drawLine(w/2-6, h-16, w/2-10, h-18);
            g.dispose();
        }
        @Override public int getIconWidth() { return w; }
        @Override public int getIconHeight() { return h; }
    }

    static abstract class Person {
        private final String name;
        private final int age;
        public Person(String n, int a) { name = n; age = a; }
        public String getName() { return name; }
        public int getAge() { return age; }
        public String basicInfo() { return name + " (" + age + ")"; }
    }

    static class Customer extends Person {
        private final String customerId;
        private final String license;
        public Customer(String id, String n, int a, String l) { super(n, a); customerId = id; license = l; }
        public String getId() { return customerId; }
        public String getLicense() { return license; }
        @Override public String basicInfo() { return customerId + " | " + getName() + " | Age: " + getAge() + " | License: " + license; }
    }

    static class Bike {
        private final String bikeId;
        private final String model;
        private final int rate;
        private boolean available = true;
        public Bike(String id, String m, int r) { bikeId = id; model = m; rate = r; }
        public String getBikeId() { return bikeId; }
        public String getModel() { return model; }
        public int getRate() { return rate; }
        public boolean isAvailable() { return available; }
        public void setAvailable(boolean a) { available = a; }
        public String toString() { return bikeId + " | " + model + " | ₹" + rate + "/day | " + (available ? "Available" : "Rented"); }
    }

    static class Rental {
        String rentalId, bikeId, customerId;
        int days, baseRent, damageFee, total;
        boolean returned = false;
        String startDate = LocalDate.now().toString();
        String returnDate = "";
        public Rental(String rid, String bid, String cid, int d, int cost) {
            rentalId = rid; bikeId = bid; customerId = cid; days = d; baseRent = cost; total = cost;
        }
    }

    static class InvalidLicenseException extends Exception { public InvalidLicenseException(String msg) { super(msg); } }

    interface RentalPolicy { boolean eligible(Customer c); int damageFee(String level); }

    static class SimplePolicy implements RentalPolicy {
        public boolean eligible(Customer c) { return c.getAge() >= 18 && c.getLicense().trim().length() >= 8; }
        public int damageFee(String level) {
            switch (String.valueOf(level)) {
                case "Minor Scratch": return 200;
                case "Moderate Damage": return 500;
                case "Heavy Damage": return 1000;
                default: return 0;
            }
        }
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> new BikeRentalSystem().setVisible(true));
    }
}
