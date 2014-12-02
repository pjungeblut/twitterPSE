/*
Erstellt unser aktuelles Datenbanklayout.
Man muss mit einem entsprechend maechtigen benutzer bei MySQL angemeldet sein. (root, Passwort siehe Mail von Paul)
*/

/* anlegen einer Datenbank Twitter */

CREATE DATABASE IF NOT EXISTS Twitter;
USE Twitter;


/* Accounts-Tabelle */
CREATE TABLE IF NOT EXISTS Accounts (
	Id INT UNSIGNED AUTO_INCREMENT NOT NULL PRIMARY KEY,
	AccountId BIGINT UNSIGNED NOT NULL,
	AccountName VARCHAR(30) CHARACTER SET utf32 COLLATE utf32_unicode_520_ci NOT NULL,
	Verified BIT NOT NULL,
	Follower BIGINT UNSIGNED NOT NULL,
	Location INT UNSIGNED NOT NULL,
	Category INT UNSIGNED,
	UnlocalizedRetweets INT UNSIGNED NOT NULL
);


/* Location-Tabelle */
CREATE TABLE IF NOT EXISTS Location (
	Id INT UNSIGNED AUTO_INCREMENT NOT NULL PRIMARY KEY,
	Name VARCHAR(50) NOT NULL,
	Parent INT UNSIGNED
);


/* Category-Tabelle */
CREATE TABLE IF NOT EXISTS Category (
	Id INT UNSIGNED AUTO_INCREMENT NOT NULL PRIMARY KEY,
	Name VARCHAR(50) NOT NULL,
	Parent INT UNSIGNED
);


/* Retweets-Tabelle */
CREATE TABLE IF NOT EXISTS Retweets (
	Id INT UNSIGNED AUTO_INCREMENT NOT NULL PRIMARY KEY,
	Account INT UNSIGNED NOT NULL,
	Location INT UNSIGNED NOT NULL,
	Counter INT UNSIGNED NOT NULL,
	Day INT UNSIGNED NOT NULL
);

/* Category-Account-Tabelle */
CREATE TABLE IF NOT EXISTS CategoryAccount (
	Id INT UNSIGNED AUTO_INCREMENT NOT NULL PRIMARY KEY,
	Account INT UNSIGNED NOT NULL,
	Category INT UNSIGNED NOT NULL
);


/* Tweets pro Account, erstmal Daten mitnehmen, evtl. spaeter loeschen */
CREATE TABLE IF NOT EXISTS Tweets (
	Id INT UNSIGNED AUTO_INCREMENT NOT NULL PRIMARY KEY,
	Account INT UNSIGNED NOT NULL,
	Counter INT UNSIGNED NOT NULL,
	Day INT UNSIGNED NOT NULL
);


CREATE TABLE IF NOT EXISTS Day (
	Id INT UNSIGNED AUTO_INCREMENT NOT NULL PRIMARY KEY,
	Day DATE NOT NULL
);


/* Fremdschluessel */
ALTER TABLE Accounts ADD CONSTRAINT FOREIGN KEY (Location) REFERENCES Location (Id);
ALTER TABLE Accounts ADD CONSTRAINT FOREIGN KEY (Category) REFERENCES Category (Id);
ALTER TABLE Location ADD CONSTRAINT FOREIGN KEY (Parent) REFERENCES Location (Id);
ALTER TABLE Category ADD CONSTRAINT FOREIGN KEY (Parent) REFERENCES Category (Id);
ALTER TABLE Retweets ADD CONSTRAINT FOREIGN KEY (Account) REFERENCES Accounts (Id);
ALTER TABLE Retweets ADD CONSTRAINT FOREIGN KEY (Location) REFERENCES Location (Id);
ALTER TABLE Tweets ADD CONSTRAINT FOREIGN KEY (Account) REFERENCES Accounts (Id);
ALTER TABLE Tweets ADD CONSTRAINT FOREIGN KEY (Day) REFERENCES Day (Id);
ALTER TABLE Retweets ADD CONSTRAINT FOREIGN KEY (Day) REFERENCES Day (Id);
ALTER TABLE CategoryAccount ADD CONSTRAINT FOREIGN KEY (Account) REFERENCES Account (Id);
ALTER TABLE CategoryAccount ADD CONSTRAINT FOREIGN KEY (Category) REFERENCES Category (Id);
/* Unikate erzwingen */
ALTER TABLE Accounts ADD CONSTRAINT uc_accountid UNIQUE(AccountId);
ALTER TABLE Retweets ADD CONSTRAINT uc_retweet UNIQUE (Account, Day, Location);
ALTER TABLE Tweets ADD CONSTRAINT uc_tweet UNIQUE (Account, Day);
ALTER TABLE Day ADD CONSTRAINT uc_day UNIQUE (Day);
ALTER TABLE Location ADD CONSTRAINT uc_location UNIQUE (Name);