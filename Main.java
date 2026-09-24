import java.io.File;
import java.math.BigInteger;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class Main {

    // ============================================================
    // POINT
    // ============================================================

    static class Point {

        BigInteger x;
        BigInteger y;

        Point(BigInteger x, BigInteger y) {
            this.x = x;
            this.y = y;
        }
    }


    // ============================================================
    // EXACT FRACTION
    // ============================================================

    static class Fraction {

        BigInteger numerator;
        BigInteger denominator;

        Fraction(BigInteger numerator, BigInteger denominator) {

            if (denominator.equals(BigInteger.ZERO)) {
                throw new ArithmeticException(
                        "Denominator cannot be zero"
                );
            }

            // Keep denominator positive
            if (denominator.signum() < 0) {
                numerator = numerator.negate();
                denominator = denominator.negate();
            }

            // Reduce fraction
            BigInteger gcd =
                    numerator.gcd(denominator);

            this.numerator =
                    numerator.divide(gcd);

            this.denominator =
                    denominator.divide(gcd);
        }


        Fraction add(Fraction other) {

            BigInteger newNumerator =
                    numerator.multiply(other.denominator)
                            .add(
                                    other.numerator.multiply(
                                            denominator
                                    )
                            );

            BigInteger newDenominator =
                    denominator.multiply(
                            other.denominator
                    );

            return new Fraction(
                    newNumerator,
                    newDenominator
            );
        }


        Fraction multiply(Fraction other) {

            BigInteger newNumerator =
                    numerator.multiply(
                            other.numerator
                    );

            BigInteger newDenominator =
                    denominator.multiply(
                            other.denominator
                    );

            return new Fraction(
                    newNumerator,
                    newDenominator
            );
        }


        boolean isInteger() {
            return denominator.equals(
                    BigInteger.ONE
            );
        }


        @Override
        public String toString() {

            if (isInteger()) {
                return numerator.toString();
            }

            return numerator + "/" + denominator;
        }
    }


    // ============================================================
    // CONVERT VALUE FROM BASE TO DECIMAL
    // ============================================================

    static BigInteger convertToDecimal(
            String value,
            int base
    ) {

        return new BigInteger(
                value,
                base
        );
    }


    // ============================================================
    // EVALUATE POLYNOMIAL USING LAGRANGE
    //
    // P(x) =
    //
    // Sum [
    //
    //   yi * Product(
    //          (x - xj) / (xi - xj)
    //       )
    //
    // ]
    // ============================================================

    static Fraction evaluatePolynomial(
            List<Point> points,
            BigInteger x
    ) {

        Fraction result =
                new Fraction(
                        BigInteger.ZERO,
                        BigInteger.ONE
                );


        for (int i = 0;
             i < points.size();
             i++) {

            Point current =
                    points.get(i);


            Fraction term =
                    new Fraction(
                            current.y,
                            BigInteger.ONE
                    );


            for (int j = 0;
                 j < points.size();
                 j++) {

                if (i == j) {
                    continue;
                }


                Point other =
                        points.get(j);


                BigInteger numerator =
                        x.subtract(
                                other.x
                        );


                BigInteger denominator =
                        current.x.subtract(
                                other.x
                        );


                Fraction factor =
                        new Fraction(
                                numerator,
                                denominator
                        );


                term =
                        term.multiply(
                                factor
                        );
            }


            result =
                    result.add(term);
        }


        return result;
    }


    // ============================================================
    // P(0)
    // ============================================================

    static Fraction calculateP0(
            List<Point> points
    ) {

        return evaluatePolynomial(
                points,
                BigInteger.ZERO
        );
    }


    // ============================================================
    // READ INTEGER FROM JSON
    // ============================================================

    static int readInteger(
            String json,
            String key
    ) {

        Pattern pattern =
                Pattern.compile(
                        "\"" + key +
                                "\"\\s*:\\s*(\\d+)"
                );

        Matcher matcher =
                pattern.matcher(json);


        if (!matcher.find()) {

            throw new RuntimeException(
                    key + " not found"
            );
        }


        return Integer.parseInt(
                matcher.group(1)
        );
    }


    // ============================================================
    // READ POINTS FROM JSON
    // ============================================================

    static List<Point> readPoints(
            String json
    ) {

        List<Point> points =
                new ArrayList<>();


        /*
         * Matches:
         *
         * "1": {
         *     "base": "10",
         *     "value": "4"
         * }
         */

        Pattern pattern =
                Pattern.compile(
                        "\"(\\d+)\"\\s*:\\s*\\{"
                                + "\\s*\"base\"\\s*:\\s*\"(\\d+)\""
                                + "\\s*,\\s*\"value\"\\s*:\\s*\"([0-9A-Za-z]+)\""
                                + "\\s*\\}"
                );


        Matcher matcher =
                pattern.matcher(json);


        while (matcher.find()) {

            BigInteger x =
                    new BigInteger(
                            matcher.group(1)
                    );


            int base =
                    Integer.parseInt(
                            matcher.group(2)
                    );


            String value =
                    matcher.group(3);


            BigInteger y =
                    convertToDecimal(
                            value,
                            base
                    );


            points.add(
                    new Point(
                            x,
                            y
                    )
            );
        }


        // Sort by x
        points.sort(
                Comparator.comparing(
                        point -> point.x
                )
        );


        return points;
    }


    // ============================================================
    // CHECK HOW MANY POINTS MATCH A POLYNOMIAL
    // ============================================================

    static int countMatchingPoints(
            List<Point> allPoints,
            List<Point> candidate
    ) {

        int count = 0;


        for (Point point : allPoints) {

            Fraction calculated =
                    evaluatePolynomial(
                            candidate,
                            point.x
                    );


            if (
                    calculated.denominator.equals(
                            BigInteger.ONE
                    )
                            &&
                    calculated.numerator.equals(
                            point.y
                    )
            ) {

                count++;
            }
        }


        return count;
    }


    // ============================================================
    // STORE BEST POLYNOMIAL
    // ============================================================

    static class BestResult {

        List<Point> points;

        int matchingPoints;

        Fraction p0;
    }


    // ============================================================
    // FIND BEST POLYNOMIAL
    // ============================================================

    static BestResult findBestPolynomial(
            List<Point> points,
            int k
    ) {

        BestResult best =
                new BestResult();


        best.points = null;
        best.matchingPoints = -1;


        List<Point> current =
                new ArrayList<>();


        generateCombinations(
                points,
                k,
                0,
                current,
                best
        );


        return best;
    }


    // ============================================================
    // GENERATE ALL K-SIZED COMBINATIONS
    // ============================================================

    static void generateCombinations(
            List<Point> points,
            int k,
            int start,
            List<Point> current,
            BestResult best
    ) {

        // --------------------------------------------------------
        // We have k points
        // --------------------------------------------------------

        if (current.size() == k) {

            int matches =
                    countMatchingPoints(
                            points,
                            current
                    );


            /*
             * If this polynomial matches more
             * points than the previous best,
             * keep it.
             */

            if (
                    best.points == null
                            ||
                    matches > best.matchingPoints
            ) {

                best.points =
                        new ArrayList<>(
                                current
                        );

                best.matchingPoints =
                        matches;

                best.p0 =
                        calculateP0(
                                current
                        );
            }


            return;
        }


        // --------------------------------------------------------
        // Generate combinations
        // --------------------------------------------------------

        int remaining =
                k - current.size();


        for (
                int i = start;
                i <= points.size() - remaining;
                i++
        ) {

            current.add(
                    points.get(i)
            );


            generateCombinations(
                    points,
                    k,
                    i + 1,
                    current,
                    best
            );


            current.remove(
                    current.size() - 1
            );
        }
    }


    // ============================================================
    // SOLVE
    // ============================================================

    static void solve(
            String json
    ) {

        int n =
                readInteger(
                        json,
                        "n"
                );


        int k =
                readInteger(
                        json,
                        "k"
                );


        List<Point> points =
                readPoints(
                        json
                );


        // --------------------------------------------------------
        // Validation
        // --------------------------------------------------------

        if (points.size() != n) {

            throw new RuntimeException(
                    "Expected "
                            + n
                            + " points, but found "
                            + points.size()
            );
        }


        if (k < 1 || k > n) {

            throw new RuntimeException(
                    "Invalid k value"
            );
        }


        // --------------------------------------------------------
        // Header
        // --------------------------------------------------------

        System.out.println(
                "n = " + n
        );

        System.out.println(
                "k = " + k
        );

        System.out.println(
                "Polynomial degree = "
                        + (k - 1)
        );


        // --------------------------------------------------------
        // Converted points
        // --------------------------------------------------------

        System.out.println();

        System.out.println(
                "Converted Points:"
        );

        System.out.println(
                "-------------------------"
        );


        for (Point point : points) {

            System.out.println(
                    "("
                            + point.x
                            + ", "
                            + point.y
                            + ")"
            );
        }


        // --------------------------------------------------------
        // Find polynomial
        // --------------------------------------------------------

        System.out.println();

        System.out.println(
                "Searching for consistent polynomial..."
        );


        BestResult best =
                findBestPolynomial(
                        points,
                        k
                );


        // --------------------------------------------------------
        // Selected points
        // --------------------------------------------------------

        System.out.println();

        System.out.println(
                "Selected Points:"
        );

        System.out.println(
                "-------------------------"
        );


        for (
                Point point :
                best.points
        ) {

            System.out.println(
                    "("
                            + point.x
                            + ", "
                            + point.y
                            + ")"
            );
        }


        // --------------------------------------------------------
        // Matching count
        // --------------------------------------------------------

        System.out.println();

        System.out.println(
                "Points matching polynomial = "
                        + best.matchingPoints
                        + " / "
                        + n
        );


        // --------------------------------------------------------
        // Constant term
        // --------------------------------------------------------

        System.out.println();

        System.out.println(
                "Constant term P(0) = "
                        + best.p0
        );


        // --------------------------------------------------------
        // Verification
        // --------------------------------------------------------

        System.out.println();

        System.out.println(
                "Verification:"
        );

        System.out.println(
                "-------------------------"
        );


        for (Point point : points) {

            Fraction calculated =
                    evaluatePolynomial(
                            best.points,
                            point.x
                    );


            String status;


            if (
                    calculated.denominator.equals(
                            BigInteger.ONE
                    )
                            &&
                    calculated.numerator.equals(
                            point.y
                    )
            ) {

                status = "PASS";

            } else {

                status = "MISMATCH";
            }


            System.out.println(
                    "x = "
                            + point.x
                            + " | Expected = "
                            + point.y
                            + " | Calculated = "
                            + calculated
                            + " | "
                            + status
            );
        }


        System.out.println();

        System.out.println(
                "Polynomial reconstructed successfully."
        );
    }


    // ============================================================
    // MAIN
    // ============================================================

    public static void main(
            String[] args
    ) {

        try {

            String filePath;


            if (args.length > 0) {

                filePath = args[0];

            } else {

                filePath = "input.json";
            }


            File file =
                    new File(
                            filePath
                    );


            // Also check src/input.json
            if (!file.exists()) {

                file =
                        new File(
                                "src/input.json"
                        );
            }


            if (!file.exists()) {

                System.out.println(
                        "ERROR: input.json not found!"
                );

                System.out.println(
                        "Current Working Directory: "
                                + System.getProperty(
                                "user.dir"
                        )
                );

                return;
            }


            String json =
                    Files.readString(
                            Path.of(
                                    file.getPath()
                            )
                    );


            solve(json);


        } catch (Exception e) {

            System.out.println(
                    "Error: "
                            + e.getMessage()
            );

            e.printStackTrace();
        }
    }
}