Select distinct Child from Veza


Update JUS set JUSOpis = REPLACE(JUSOpis, 'č', 'a')    where JUSId = "1"

CREATE VIRTUAL TABLE "vJUS" USING fts4(
	`JUSId`	TEXT,
	`Direktiva`	TEXT,
	`JUSopis`	TEXT,
	`Glasnik`	TEXT,
	`Naredba`	INT,
	`Locked`	INT DEFAULT 0,
	`Link-n`	TEXT,
	`Link-d`	TEXT,
	`JUSgodina`	INTEGER,
	`Mandatory`	INTEGER DEFAULT 0,
	`Napomena`	TEXT,
	`Fali`	INTEGER DEFAULT 0,
	PRIMARY KEY(JUSId)
)

INSERT INTO vJUS (`JUSId`,
	`Direktiva`,
	`JUSopis`,
	`Glasnik`,
	`Naredba`,
	`Locked`,
	`Link-n`,
	`Link-d`,
	`JUSgodina`,	
	`Mandatory`,
	`Napomena`,
	`Fali`	)
	SELECT `JUSId`,
	`Direktiva`,
	`JUSopis`,
	`Glasnik`,
	`Naredba`,
	`Locked`,
	`Link-n`,
	`Link-d`,
	`JUSgodina`,	
	`Mandatory`,
	`Napomena`,
	`Fali` FROM JUS;

SELECT * FROM vJUS WHERE JUSOpis MATCH 'mašina'
