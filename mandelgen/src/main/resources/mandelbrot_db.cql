CREATE TABLE mandelbrot.images (
	id int PRIMARY KEY,
	width bigint,
	height bigint,
	gentime timestamp
);

CREATE TABLE mandelbrot.sets (
	id int,
	sector int,
	x int,
	y int,
	state frozen <tuple<double, double>>,
	iteration int,
	PRIMARY KEY ((id, sector), x, y)
);