package com.sathya.co.biztrack.entity;

public enum Role {
    OWNER,
    EMPLOYEE,
    VIEWER;

    public boolean canAdd()           { return this == OWNER || this == EMPLOYEE; }
    public boolean canModify()        { return this == OWNER; }          // edit + delete
    public boolean canViewReports()   { return this != EMPLOYEE; }
    public boolean canManageMembers() { return this == OWNER; }
}