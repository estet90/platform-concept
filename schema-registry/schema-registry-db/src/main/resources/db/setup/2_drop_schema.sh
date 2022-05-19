#!/bin/bash

psql -U postgres -h localhost -d schema_registry -f 2_drop_schema.sql
