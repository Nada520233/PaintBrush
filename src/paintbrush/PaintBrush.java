package paintbrush;

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import javax.imageio.ImageIO;
import java.util.ArrayList;
import java.util.Stack;

public class PaintBrush extends JFrame {

    private boolean drawing;
    private boolean dotted;
    private boolean filled;
    private boolean erasing;
    private Color currentColor;
    private DrawingMode currentMode;
    private Stack<Shape> undoStack;
    private Stack<Shape> redoStack;
    private Point startPoint;
    private Point endPoint;

    private enum DrawingMode {
        LINE, RECTANGLE, OVAL, TRIANGLE, PEN, NONE
    }

    public PaintBrush() {
        setTitle("Paint Brush");
        setSize(900, 650);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLocationRelativeTo(null);

        currentColor = Color.BLACK;
        currentMode = DrawingMode.LINE;
        undoStack = new Stack<>();
        redoStack = new Stack<>();
        dotted = false;
        filled = false;
        erasing = false;

        Canvas canvas = new Canvas();
        add(canvas, BorderLayout.CENTER);

        // Top panel for buttons with a modern design
        JPanel panel = new JPanel();
        panel.setLayout(new GridLayout(1, 0, 10, 0));  // Grid layout for horizontal arrangement with gaps
        panel.setBackground(new Color(34, 40, 49));  // Dark blue background for a modern look
        add(panel, BorderLayout.NORTH);

        // Buttons for drawing tools
        panel.add(createButton("Pen", e -> setCurrentMode(DrawingMode.PEN)));
        panel.add(createButton("Line", e -> setCurrentMode(DrawingMode.LINE)));
        panel.add(createButton("Rectangle", e -> setCurrentMode(DrawingMode.RECTANGLE)));
        panel.add(createButton("Oval", e -> setCurrentMode(DrawingMode.OVAL)));
        panel.add(createButton("Triangle", e -> setCurrentMode(DrawingMode.TRIANGLE)));
        panel.add(createButton("Dotted", e -> toggleDotted()));
        panel.add(createButton("Fill", e -> toggleFill()));
        panel.add(createButton("Color", e -> chooseColor()));
        panel.add(createButton("Undo", e -> undo()));
        panel.add(createButton("Redo", e -> redo()));
        panel.add(createButton("Clear", e -> clearCanvas()));
        panel.add(createButton("Save", e -> saveCanvas()));  // Save button

        setVisible(true);
    }

    private class Canvas extends JPanel {
        private ArrayList<Point> freeHandPoints = new ArrayList<>();

        public Canvas() {
            setBackground(Color.WHITE);

            addMouseListener(new MouseAdapter() {
                @Override
                public void mousePressed(MouseEvent e) {
                    if (erasing) {
                        eraseShapeAt(e.getPoint()); // Erase shape when erasing mode is on
                    } else {
                        drawing = true;
                        startPoint = e.getPoint();
                        freeHandPoints.clear();
                        freeHandPoints.add(startPoint);
                    }
                }

                @Override
                public void mouseReleased(MouseEvent e) {
                    if (!erasing) {
                        endPoint = e.getPoint();
                        if (currentMode != DrawingMode.PEN) {
                            Shape shape = createShape(startPoint, endPoint);
                            if (shape != null) {
                                undoStack.push(shape);
                            }
                        } else {
                            undoStack.push(new FreeHand(new ArrayList<>(freeHandPoints), currentColor, dotted));
                        }
                        repaint();
                        drawing = false;
                    }
                }
            });

            addMouseMotionListener(new MouseAdapter() {
                @Override
                public void mouseDragged(MouseEvent e) {
                    if (currentMode == DrawingMode.PEN && !erasing) {
                        freeHandPoints.add(e.getPoint());
                    } else if (!erasing) {
                        endPoint = e.getPoint();
                    }
                    repaint();
                }
            });
        }

        @Override
        protected void paintComponent(Graphics g) {
            super.paintComponent(g);

            Graphics2D g2d = (Graphics2D) g;

            for (Shape shape : undoStack) {
                shape.draw(g2d);
            }

            if (drawing && currentMode != DrawingMode.PEN && !erasing) {
                Shape previewShape = createShape(startPoint, endPoint);
                if (previewShape != null) {
                    previewShape.draw(g2d);
                }
            } else if (currentMode == DrawingMode.PEN && !erasing) {
                new FreeHand(freeHandPoints, currentColor, dotted).draw(g2d);
            }
        }

        private Shape createShape(Point start, Point end) {
            switch (currentMode) {
                case LINE:
                    return new Line(start, end, currentColor, dotted, filled);
                case RECTANGLE:
                    return new Rectangle(start, end, currentColor, dotted, filled);
                case OVAL:
                    return new Oval(start, end, currentColor, dotted, filled);
                case TRIANGLE:
                    return new Triangle(start, end, currentColor, dotted, filled);
                default:
                    return null;
            }
        }

        private void eraseShapeAt(Point point) {
            Stack<Shape> tempStack = new Stack<>();
            boolean shapeErased = false;

            for (Shape shape : undoStack) {
                if (shape.contains(point)) {
                    shapeErased = true;
                } else {
                    tempStack.push(shape);
                }
            }

            if (shapeErased) {
                undoStack.clear();
                undoStack.addAll(tempStack);
            }
            repaint();
        }
    }

    private JButton createButton(String text, ActionListener listener) {
        JButton button = new JButton(text);
        button.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        button.setBackground(new Color(50, 60, 78));
        button.setForeground(Color.WHITE);
        button.setPreferredSize(new Dimension(120, 40));
        button.setFocusPainted(false);
        button.setBorder(BorderFactory.createEmptyBorder(10, 15, 10, 15));

        button.addMouseListener(new MouseAdapter() {
            public void mouseEntered(MouseEvent e) {
                button.setBackground(new Color(70, 80, 100));
            }

            public void mouseExited(MouseEvent e) {
                button.setBackground(new Color(50, 60, 78));
            }
        });

        button.addActionListener(listener);
        return button;
    }

    private void chooseColor() {
        currentColor = JColorChooser.showDialog(this, "Choose a Color", currentColor);
    }

    private void clearCanvas() {
        undoStack.clear();
        redoStack.clear();
        repaint();
    }

    private void undo() {
        if (!undoStack.isEmpty()) {
            redoStack.push(undoStack.pop());
            repaint();
        }
    }

    private void redo() {
        if (!redoStack.isEmpty()) {
            undoStack.push(redoStack.pop());
            repaint();
        }
    }

    private void setCurrentMode(DrawingMode mode) {
        currentMode = mode;
    }

    private void toggleDotted() {
        dotted = !dotted;
    }

    private void toggleFill() {
        filled = !filled;
    }

    private void toggleErase() {
        erasing = !erasing;
    }

    // Save the current canvas as an image
    private void saveCanvas() {
        try {
            // Create a buffered image to hold the canvas content
            BufferedImage image = new BufferedImage(getWidth(), getHeight(), BufferedImage.TYPE_INT_ARGB);
            Graphics2D g2d = image.createGraphics();
            paint(g2d);  // Draw the entire window content onto the image
            g2d.dispose();

            // Use JFileChooser to allow the user to select where to save the image
            JFileChooser fileChooser = new JFileChooser();
            fileChooser.setDialogTitle("Save Image");
            fileChooser.setSelectedFile(new File("drawing.png"));  // Default file name
            int result = fileChooser.showSaveDialog(this);
            if (result == JFileChooser.APPROVE_OPTION) {
                File file = fileChooser.getSelectedFile();
                ImageIO.write(image, "PNG", file);  // Save the image as PNG
                JOptionPane.showMessageDialog(this, "Image saved successfully!", "Success", JOptionPane.INFORMATION_MESSAGE);
            }
        } catch (IOException e) {
            JOptionPane.showMessageDialog(this, "Error saving image: " + e.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    private interface Shape {
        void draw(Graphics2D g);
        boolean contains(Point point);
    }

    private static class Line implements Shape {
        private final Point start, end;
        private final Color color;
        private final boolean dotted;
        private final boolean filled;

        public Line(Point start, Point end, Color color, boolean dotted, boolean filled) {
            this.start = start;
            this.end = end;
            this.color = color;
            this.dotted = dotted;
            this.filled = filled;
        }

        @Override
        public void draw(Graphics2D g) {
            g.setColor(color);
            if (dotted) {
                g.setStroke(new BasicStroke(1, BasicStroke.CAP_BUTT, BasicStroke.JOIN_BEVEL, 0, new float[]{5}, 0));
            } else {
                g.setStroke(new BasicStroke());
            }
            g.drawLine(start.x, start.y, end.x, end.y);
        }

        @Override
        public boolean contains(Point point) {
            return false;
        }
    }

    private static class Rectangle implements Shape {
        private final Point start, end;
        private final Color color;
        private final boolean dotted;
        private final boolean filled;

        public Rectangle(Point start, Point end, Color color, boolean dotted, boolean filled) {
            this.start = start;
            this.end = end;
            this.color = color;
            this.dotted = dotted;
            this.filled = filled;
        }

        @Override
        public void draw(Graphics2D g) {
            g.setColor(color);
            if (dotted) {
                g.setStroke(new BasicStroke(1, BasicStroke.CAP_BUTT, BasicStroke.JOIN_BEVEL, 0, new float[]{5}, 0));
            } else {
                g.setStroke(new BasicStroke());
            }
            if (filled) {
                g.fillRect(Math.min(start.x, end.x), Math.min(start.y, end.y),
                        Math.abs(start.x - end.x), Math.abs(start.y - end.y));
            } else {
                g.drawRect(Math.min(start.x, end.x), Math.min(start.y, end.y),
                        Math.abs(start.x - end.x), Math.abs(start.y - end.y));
            }
        }

        @Override
        public boolean contains(Point point) {
            return new java.awt.Rectangle(Math.min(start.x, end.x), Math.min(start.y, end.y),
                    Math.abs(start.x - end.x), Math.abs(start.y - end.y)).contains(point);
        }
    }

    private static class Oval implements Shape {
        private final Point start, end;
        private final Color color;
        private final boolean dotted;
        private final boolean filled;

        public Oval(Point start, Point end, Color color, boolean dotted, boolean filled) {
            this.start = start;
            this.end = end;
            this.color = color;
            this.dotted = dotted;
            this.filled = filled;
        }

        @Override
        public void draw(Graphics2D g) {
            g.setColor(color);
            if (dotted) {
                g.setStroke(new BasicStroke(1, BasicStroke.CAP_BUTT, BasicStroke.JOIN_BEVEL, 0, new float[]{5}, 0));
            } else {
                g.setStroke(new BasicStroke());
            }
            if (filled) {
                g.fillOval(Math.min(start.x, end.x), Math.min(start.y, end.y),
                        Math.abs(start.x - end.x), Math.abs(start.y - end.y));
            } else {
                g.drawOval(Math.min(start.x, end.x), Math.min(start.y, end.y),
                        Math.abs(start.x - end.x), Math.abs(start.y - end.y));
            }
        }

        @Override
        public boolean contains(Point point) {
            return new java.awt.geom.Ellipse2D.Double(Math.min(start.x, end.x), Math.min(start.y, end.y),
                    Math.abs(start.x - end.x), Math.abs(start.y - end.y)).contains(point);
        }
    }

    private static class Triangle implements Shape {
        private final Point start, end;
        private final Color color;
        private final boolean dotted;
        private final boolean filled;

        public Triangle(Point start, Point end, Color color, boolean dotted, boolean filled) {
            this.start = start;
            this.end = end;
            this.color = color;
            this.dotted = dotted;
            this.filled = filled;
        }

        @Override
        public void draw(Graphics2D g) {
            g.setColor(color);
            if (dotted) {
                g.setStroke(new BasicStroke(1, BasicStroke.CAP_BUTT, BasicStroke.JOIN_BEVEL, 0, new float[]{5}, 0));
            } else {
                g.setStroke(new BasicStroke());
            }
            int[] xPoints = {start.x, start.x + (end.x - start.x) / 2, end.x};
            int[] yPoints = {end.y, start.y, end.y};
            if (filled) {
                g.fillPolygon(xPoints, yPoints, 3);
            } else {
                g.drawPolygon(xPoints, yPoints, 3);
            }
        }

        @Override
        public boolean contains(Point point) {
            return false;
        }
    }

    private static class FreeHand implements Shape {
        private final ArrayList<Point> points;
        private final Color color;
        private final boolean dotted;

        public FreeHand(ArrayList<Point> points, Color color, boolean dotted) {
            this.points = points;
            this.color = color;
            this.dotted = dotted;
        }

        @Override
        public void draw(Graphics2D g) {
            g.setColor(color);
            if (dotted) {
                g.setStroke(new BasicStroke(1, BasicStroke.CAP_BUTT, BasicStroke.JOIN_BEVEL, 0, new float[]{5}, 0));
            } else {
                g.setStroke(new BasicStroke());
            }
            for (int i = 0; i < points.size() - 1; i++) {
                Point p1 = points.get(i);
                Point p2 = points.get(i + 1);
                g.drawLine(p1.x, p1.y, p2.x, p2.y);
            }
        }

        @Override
        public boolean contains(Point point) {
            return false;
        }
    }

    public static void main(String[] args) {
        new PaintBrush();
    }
}
