create table if not exists interedition_text (
  id bigint primary key,
  text_content clob not null
);
create table if not exists interedition_text_annotation (
  id bigint primary key,
  anno_data clob not null
);
create table if not exists interedition_text_annotation_target (
  annotation_id bigint not null references interedition_text_annotation (id) on delete cascade,
  text_id bigint not null,
  range_start bigint not null,
  range_end bigint not null
);
create index if not exists interedition_text_annotation_target_text on interedition_text_annotation_target (text_id);
create index if not exists interedition_text_annotation_target_range on interedition_text_annotation_target (range_start, range_end);
