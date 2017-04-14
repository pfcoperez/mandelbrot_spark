CREATE TABLE mandelbrot.fractalset (
	sector int,
	x int,
	y int,
	state frozen <tuple<double, double>>,
	iteration int,
	PRIMARY KEY (sector, x, y)
);