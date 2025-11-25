box.cfg { listen = 3301 }

box.schema.user.create('test_user', { password = 'test_password', if_not_exists = true })
box.schema.user.grant('test_user', 'read, write, execute', 'universe', nil, { if_not_exists = true })
box.schema.user.create('replicator', { password = 'password' })
box.schema.user.grant('replicator', 'replication', 'universe', nil, { if_not_exists = true })
