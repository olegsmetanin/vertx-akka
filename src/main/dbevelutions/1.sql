# Create UserSessions

# --- !Ups

DROP TABLE UserSessions;

CREATE TABLE UserSessions (
    id integer NOT NULL,
    userid varchar(255) NOT NULL,
    sessionid varchar(255) NOT NULL,
    PRIMARY KEY (id)
);

DROP TABLE Films;

CREATE TABLE Films
(
  code character(5),
  title character varying(40),
  did integer,
  date_prod date,
  kind character varying(10),
  len interval hour to minute,
  CONSTRAINT production UNIQUE (date_prod)
);

INSERT INTO Films(
            code, title, did, date_prod, kind, len)
    VALUES ('CODE1', 'Title1', 1, now(), 'Kind1', '00:40:00');



# --- !Downs

DROP TABLE UserSessions;

DROP TABLE Films