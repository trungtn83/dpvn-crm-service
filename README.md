# dpvn-crm-service

# 2025-05-20

Update all user from kiotviet(duocphamvietnhat) to new kiotviet(tranngocm)
CRM system

- clone idf in user table to idf_old
  alter table "user" add column idf_old int8;
  update "user" u set idf_old = idf;

WMS system

- update user idf to new one in table Invoice and Order
