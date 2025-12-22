local vshard = require('vshard')
local crud = require('crud')

crud.init_storage()
box.once('person', function()
    local space = box.schema.space.create("person", {
        if_not_exists = true,
        format = {
            { 'id', type = 'number' },
            { 'is_married', type = 'boolean', is_nullable = true },
            { 'name', type = 'string' },
            { 'bucket_id', 'unsigned' },
        },
        if_not_exists = true
    })
    space:create_index('pk', { parts = { 'id' }, if_not_exists = true })
    space:create_index('bucket_id', { parts = { 'bucket_id' }, unique = false, if_not_exists = true })
    space:create_index('is_married_idx', { parts = { 'is_married' }, unique = false, if_not_exists = true })

    local complex_space = box.schema.space.create("complex_person", {
        if_not_exists = true,
        format = {
            { 'id', type = 'number' },
            { 'second_id', type = 'uuid' },
            { 'is_married', type = 'boolean', is_nullable = true },
            { 'name', type = 'string' },
            { 'bucket_id', 'unsigned' },
        },
        if_not_exists = true
    })
    complex_space:create_index('pk', { parts = { 'id', 'second_id' }, if_not_exists = true })
    complex_space:create_index('bucket_id', { parts = { 'bucket_id' }, unique = false, if_not_exists = true })
    complex_space:create_index('is_married_idx', { parts = { 'is_married' }, unique = false, if_not_exists = true })

    local nested_space = box.schema.space.create("nested_person", {
        if_not_exists = true,
        format = {
            { 'id', type = 'number' },
            { 'name', type = 'string' },
            { 'friends', type = 'array'},
            { 'buys', type = 'map'},
            { 'husband', type = 'array'},
            { 'child', type = 'map'},
            { 'bucket_id', 'unsigned' },
        },
        if_not_exists = true
    })
    nested_space:create_index('pk', { parts = { 'id' }, if_not_exists = true })
    nested_space:create_index('bucket_id', { parts = { 'bucket_id' }, unique = false, if_not_exists = true })
end)
