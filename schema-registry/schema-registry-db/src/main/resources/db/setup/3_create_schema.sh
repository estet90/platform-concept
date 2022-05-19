#!/bin/bash

psql -U schema_registry_admin -h localhost -d schema_registry -f 3_create_schema.sql
