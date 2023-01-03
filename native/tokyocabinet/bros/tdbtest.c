/*************************************************************************************************
 * Writing test of Trivial Database
 *************************************************************************************************/


#include <sys/types.h>
#include <fcntl.h>
#include <stdlib.h>
#include <stdio.h>
#include <string.h>
#include <tdb.h>

#undef TRUE
#define TRUE           1                 /* boolean true */
#undef FALSE
#define FALSE          0                 /* boolean false */

#define RECBUFSIZ      32                /* buffer for records */


/* global variables */
const char *progname;                    /* program name */
int showprgr;                            /* whether to show progression */


/* function prototypes */
int main(int argc, char **argv);
void usage(void);
int runwrite(int argc, char **argv);
int runread(int argc, char **argv);
int dowrite(char *name, int rnum);
int doread(char *name, int rnum);


/* main routine */
int main(int argc, char **argv){
  int rv;
  progname = argv[0];
  showprgr = TRUE;
  if(getenv("HIDEPRGR")) showprgr = FALSE;
  if(argc < 2) usage();
  rv = 0;
  if(!strcmp(argv[1], "write")){
    rv = runwrite(argc, argv);
  } else if(!strcmp(argv[1], "read")){
    rv = runread(argc, argv);
  } else {
    usage();
  }
  return rv;
}


/* print the usage and exit */
void usage(void){
  fprintf(stderr, "%s: test cases for Trivial Database\n", progname);
  fprintf(stderr, "\n");
  fprintf(stderr, "usage:\n");
  fprintf(stderr, "  %s write name rnum\n", progname);
  fprintf(stderr, "  %s read name rnum\n", progname);
  fprintf(stderr, "\n");
  exit(1);
}


/* parse arguments of write command */
int runwrite(int argc, char **argv){
  char *name, *rstr;
  int i, rnum, rv;
  name = NULL;
  rstr = NULL;
  rnum = 0;
  for(i = 2; i < argc; i++){
    if(!name && argv[i][0] == '-'){
      usage();
    } else if(!name){
      name = argv[i];
    } else if(!rstr){
      rstr = argv[i];
    } else {
      usage();
    }
  }
  if(!name || !rstr) usage();
  rnum = atoi(rstr);
  if(rnum < 1) usage();
  rv = dowrite(name, rnum);
  return rv;
}


/* parse arguments of read command */
int runread(int argc, char **argv){
  char *name, *rstr;
  int i, rnum, rv;
  name = NULL;
  rstr = NULL;
  rnum = 0;
  for(i = 2; i < argc; i++){
    if(!name && argv[i][0] == '-'){
      usage();
    } else if(!name){
      name = argv[i];
    } else if(!rstr){
      rstr = argv[i];
    } else {
      usage();
    }
  }
  if(!name || !rstr) usage();
  rnum = atoi(rstr);
  if(rnum < 1) usage();
  rv = doread(name, rnum);
  return rv;
}


/* perform write command */
int dowrite(char *name, int rnum){
  TDB_CONTEXT *tdb;
  TDB_DATA key, record;
  int i, err, len;
  char buf[RECBUFSIZ];
  if(showprgr) printf("<Writing Test>\n  name=%s  rnum=%d\n\n", name, rnum);
  /* open a database */
  if(!(tdb = tdb_open(name, rnum * 2, 0, O_RDWR | O_CREAT | O_TRUNC, 00644))){
    fprintf(stderr, "tdb_open failed\n");
    return 1;
  }
  err = FALSE;
  /* loop for each record */
  for(i = 1; i <= rnum; i++){
    len = sprintf(buf, "%08d", i);
    key.dptr = (unsigned char *)buf;
    key.dsize = len;
    record.dptr = (unsigned char *)buf;
    record.dsize = len;
    /* store a record */
    if(tdb_store(tdb, key, record, TDB_REPLACE) != 0){
      fprintf(stderr, "tdb_store failed\n");
      err = TRUE;
      break;
    }
    /* print progression */
    if(showprgr && rnum > 250 && i % (rnum / 250) == 0){
      putchar('.');
      fflush(stdout);
      if(i == rnum || i % (rnum / 10) == 0){
        printf(" (%08d)\n", i);
        fflush(stdout);
      }
    }
  }
  /* close the database */
  tdb_close(tdb);
  if(showprgr && !err) printf("ok\n\n");
  return err ? 1 : 0;
}


/* perform read command */
int doread(char *name, int rnum){
  TDB_CONTEXT *tdb;
  TDB_DATA key, record;
  int i, err, len;
  char buf[RECBUFSIZ];
  if(showprgr) printf("<Writing Test>\n  name=%s  rnum=%d\n\n", name, rnum);
  /* open a database */
  if(!(tdb = tdb_open(name, rnum * 2, 0, O_RDONLY, 00644))){
    fprintf(stderr, "tdb_open failed\n");
    return 1;
  }
  err = FALSE;
  /* loop for each record */
  for(i = 1; i <= rnum; i++){
    len = sprintf(buf, "%08d", i);
    key.dptr = (unsigned char *)buf;
    key.dsize = len;
    /* retrieve a record */
    record = tdb_fetch(tdb, key);
    if(!record.dptr){
      fprintf(stderr, "tdb_fetch failed\n");
      err = TRUE;
      break;
    }
    free(record.dptr);
    /* print progression */
    if(showprgr && rnum > 250 && i % (rnum / 250) == 0){
      putchar('.');
      fflush(stdout);
      if(i == rnum || i % (rnum / 10) == 0){
        printf(" (%08d)\n", i);
        fflush(stdout);
      }
    }
  }
  /* close the database */
  tdb_close(tdb);
  if(showprgr && !err) printf("ok\n\n");
  return err ? 1 : 0;
}



/* END OF FILE */
