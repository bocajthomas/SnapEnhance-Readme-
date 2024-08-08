use std::{io::Error, string::FromUtf8Error};

#[derive(Debug, Clone)]
pub struct ModuleTag {
    tag_type: u8,
    buffer: Vec<u8>,
}

impl ModuleTag {
    pub fn new(module_type: u8, buffer: Vec<u8>) -> ModuleTag {
        ModuleTag {
            tag_type: module_type,
            buffer,
        }
    }

    pub fn to_string(&self) -> Result<String, FromUtf8Error> {
        Ok(String::from_utf8(self.buffer.clone())?)
    }

    pub fn get_tag_type(&self) -> u8 {
        self.tag_type
    }

    pub fn get_size(&self) -> usize {
        self.buffer.len()
    }

    pub fn get_buffer(&self) -> &Vec<u8> {
        &self.buffer
    }

    pub fn set_buffer(&mut self, buffer: Vec<u8>) {
        self.buffer = buffer;
    }
}

#[derive(Debug, Clone)]
pub struct ComposerModule {
    tags: Vec<(ModuleTag, ModuleTag)>, // file name => file content
}

impl ComposerModule {
    pub fn parse(buffer: Vec<u8>) -> Result<ComposerModule, Error> {
        let mut offset = 0;
        let magic = u32::from_be_bytes([buffer[offset], buffer[offset + 1], buffer[offset + 2], buffer[offset + 3]]);

        offset += 4;

        if magic != 0x33c60001 {
            return Err(Error::new(std::io::ErrorKind::InvalidData, "Invalid magic"));
        }

        // skip content length
        offset += 4;

        let mut tags = Vec::new();

        loop {
            if offset >= buffer.len() {
                break;
            }

            fn read_u24(buffer: &Vec<u8>, offset: &mut usize) -> Result<u32, Error> {
                let b1 = buffer[*offset] as u32;
                let b2 = buffer[*offset + 1] as u32;
                let b3 = buffer[*offset + 2] as u32;
                *offset += 3;
                Ok(b1 | (b2 << 8) | (b3 << 16))
            }

            let tag_size = read_u24(&buffer, &mut offset)?;
            let tag_type = buffer[offset];
            offset += 1;
            let tag_buffer = buffer[offset..offset + tag_size as usize].to_vec();
            offset += tag_size as usize;

            let padding = 4 - (tag_size % 4);

            if padding != 4 {
                offset += padding as usize;
            }

            tags.push(ModuleTag::new(tag_type, tag_buffer));
        }

        let tags = tags.chunks(2).map(|chunk| {
            (chunk[0].clone(), chunk[1].clone())
        }).collect();

        Ok(ComposerModule {
            tags,
        })
    }

    pub fn to_bytes(&self) -> Vec<u8> {
        let mut tag_buffer = Vec::new();

        fn write_u24(buffer: &mut Vec<u8>, value: u32) {
            buffer.push((value & 0xff) as u8);
            buffer.push(((value >> 8) & 0xff) as u8);
            buffer.push(((value >> 16) & 0xff) as u8);
        }

        fn write_tag(buffer: &mut Vec<u8>, tag: ModuleTag) {
            write_u24(buffer, tag.get_size() as u32);
            buffer.push(tag.get_tag_type());
            buffer.extend(tag.get_buffer());

            let padding = 4 - (tag.get_size() % 4);

            if padding != 4 {
                for _ in 0..padding {
                    buffer.push(0);
                }
            }
        }

        for (tag1, tag2) in &self.tags {
            write_tag(&mut tag_buffer, tag1.clone());
            write_tag(&mut tag_buffer, tag2.clone());
        }

        let mut buffer = Vec::new();

        buffer.extend_from_slice(&[0x33, 0xc6, 0, 1]);
        buffer.extend_from_slice(&(tag_buffer.len() as u32).to_le_bytes());
        buffer.extend(tag_buffer);

        buffer
    }

    pub fn get_tags(&self) -> Vec<(ModuleTag, ModuleTag)> {
        self.tags.clone()
    }

    pub fn set_tags(&mut self, tags: Vec<(ModuleTag, ModuleTag)>) {
        self.tags = tags;
    }
}

