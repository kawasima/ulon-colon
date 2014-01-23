CREATE OR REPLACE TRIGGER emp_after_insert
AFTER INSERT
   ON emp
   FOR EACH ROW
BEGIN
   DBMS_ALERT.SIGNAL('NEW_EMP', :new.empno || ' ' || :new.ename);
END;
/