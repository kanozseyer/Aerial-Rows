
## Export process v5 update

The user again asked for a reliability/efficiency/speed review before continuing. C8 testing showed that the remaining weak link was manual transfer size, not archive validation. The process was upgraded to lossless deterministic `gzip+base64` transport for new raw reports, typically cutting Markdown storage/connector payload by about half. Original report SHA-256 remains authoritative; the repository validator decodes/decompresses and verifies the exact original bytes. C1-C7 remain valid raw UTF-8 archive entries. C8 is the first report stored in the compressed representation.
