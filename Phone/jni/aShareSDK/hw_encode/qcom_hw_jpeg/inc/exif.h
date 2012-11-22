/*========================================================================


*//** @file exif.h

@par EXTERNALIZED FUNCTIONS
  (none)

@par INITIALIZATION AND SEQUENCING REQUIREMENTS
  (none)

Copyright (C) 2008-09 QUALCOMM Incorporated.
All Rights Reserved. QUALCOMM Proprietary and Confidential.
*//*====================================================================== */

/*========================================================================
                             Edit History

$Header:

when       who     what, where, why
--------   ---     -------------------------------------------------------
08/03/09   vma     Switched to use the os abstraction layer (os_*)
06/30/09   vma     Added a function to convert 16-bit tag ID's to
                   the 32-bit proprietary ones.
05/19/09   vma     Fixed typo in LATITUDE.
10/09/08   vma     Filled up comments.
07/14/08   vma     Created file.

========================================================================== */

#ifndef _EXIF_H
#define _EXIF_H

#include "os_int.h"
#include "os_types.h"

/* -----------------------------------------------------------------------
** Type Declarations
** ----------------------------------------------------------------------- */

/* Exif Info (opaque definition) */
struct exif_info_t;
typedef struct exif_info_t * exif_info_obj_t;

/* Exif Tag ID */
typedef uint32_t exif_tag_id_t;

/* Exif Tag Data Type */
typedef enum
{
    EXIF_BYTE      = 1,
    EXIF_ASCII     = 2,
    EXIF_SHORT     = 3,
    EXIF_LONG      = 4,
    EXIF_RATIONAL  = 5,
    EXIF_UNDEFINED = 7,
    EXIF_SLONG     = 9,
    EXIF_SRATIONAL = 10

} exif_tag_type_t;

/* Exif Rational Data Type */
typedef struct
{
    uint32_t  num;    // Numerator
    uint32_t  denom;  // Denominator

} rat_t;

/* Exif Signed Rational Data Type */
typedef struct
{
    int32_t  num;    // Numerator
    int32_t  denom;  // Denominator

} srat_t;

/* Exif Tag Entry
 * Used in exif_set_tag as an input argument and
 * in exif_get_tag as an output argument. */
typedef struct
{
    /* The Data Type of the Tag *
     * Rational, etc */
    exif_tag_type_t type;

    /* Copy
     * This field is used when a user pass this structure to
     * be stored in an exif_info_t via the exif_set_tag method.
     * The routine would look like this field and decide whether
     * it is necessary to make a copy of the data pointed by this
     * structure (all string and array types).
     * If this field is set to false, only a pointer to the actual
     * data is retained and it is the caller's responsibility to
     * ensure the validity of the data before the exif_info_t object
     * is destroyed.
     */
    uint8_t copy;

    /* Data count
     * This indicates the number of elements of the data. For example, if
     * the type is EXIF_BYTE and the count is 1, that means the actual data
     * is one byte and is accessible by data._byte. If the type is EXIF_BYTE
     * and the count is more than one, the actual data is contained in an
     * array and is accessible by data._bytes. In case of EXIF_ASCII, it
     * indicates the string length and in case of EXIF_UNDEFINED, it indicates
     * the length of the array.
     */
    uint32_t count;

    /* Data
     * A union which covers all possible data types. The user should pick
     * the right field to use depending on the data type and the count.
     * See in-line comment below.
     */
    union
    {
        char      *_ascii;      // EXIF_ASCII (count indicates string length)
        uint8_t   *_bytes;      // EXIF_BYTE  (count > 1)
        uint8_t    _byte;       // EXIF_BYTE  (count = 1)
        uint16_t  *_shorts;     // EXIF_SHORT (count > 1)
        uint16_t   _short;      // EXIF_SHORT (count = 1)
        uint32_t  *_longs;      // EXIF_LONG  (count > 1)
        uint32_t   _long;       // EXIF_LONG  (count = 1)
        rat_t     *_rats;       // EXIF_RATIONAL  (count > 1)
        rat_t      _rat;        // EXIF_RATIONAL  (count = 1)
        uint8_t   *_undefined;  // EXIF_UNDEFINED (count indicates length)
        int32_t   *_slongs;     // EXIF_SLONG (count > 1)
        int32_t    _slong;      // EXIF_SLONG (count = 1)
        srat_t    *_srats;      // EXIF_SRATIONAL (count > 1)
        srat_t     _srat;       // EXIF_SRATIONAL (count = 1)

    } data;

} exif_tag_entry_t;

/* =======================================================================
**                          Function Definitions
** ======================================================================= */

/******************************************************************************
 * Function: exif_init
 * Description: Initializes the Exif Info object. Dynamic allocations take
 *              place during the call. One should always call exif_destroy to
 *              clean up the Exif Info object after use.
 * Input parameters:
 *   p_obj - The pointer to the Exif Info object to be initialized.
 *
 * Return values:
 *     JPEGERR_SUCCESS
 *     JPEGERR_ENULLPTR
 *     JPEGERR_EMALLOC
 * (See jpegerr.h for description of error values.)
 * Notes: none
 *****************************************************************************/
int exif_init(exif_info_obj_t *p_obj);

/******************************************************************************
 * Function: exif_set_tag
 * Description: Inserts or modifies an Exif tag to the Exif Info object. Typical
 *              use is to call this function multiple times - to insert all the
 *              desired Exif Tags individually to the Exif Info object and
 *              then pass the info object to the Jpeg Encoder object so
 *              the inserted tags would be emitted as tags in the Exif header.
 * Input parameters:
 *   obj       - The Exif Info object where the tag would be inserted to or
 *               modified from.
 *   tag_id    - The Exif Tag ID of the tag to be inserted/modified.
 *   p_entry   - The pointer to the tag entry structure which contains the
 *               details of tag. The pointer can be set to NULL to un-do
 *               previous insertion for a certain tag.
 * Return values:
 *     JPEGERR_SUCCESS
 *     JPEGERR_ENULLPTR
 *     JPEGERR_EFAILED
 * (See jpegerr.h for description of error values.)
 * Notes: none
 *****************************************************************************/
int exif_set_tag(exif_info_obj_t    obj,
                 exif_tag_id_t      tag_id,
                 exif_tag_entry_t  *p_entry);

/******************************************************************************
 * Function: exif_get_tag
 * Description: Queries the stored Exif tags in the Exif Info object. Typical
 *              use is to obtain an Exif Info object from the Jpeg Decoder
 *              object which parsed an Exif header then make multiple calls
 *              to this function to individually obtain each tag present in
 *              the header. Note that all memory allocations made to hold the
 *              exif tags are owned by the Exif Info object and they will be
 *              released when exif_destroy is called on the Exif Info object.
 *              Any app needing to preserve those data for later use should
 *              make copies of the tag contents.
 *              If the requested TAG is not present from the file, an
 *              JPEGERR_TAGABSENT will be returned.
 * Input parameters:
 *   obj       - The Exif Info object where the Exif tags should be obtained.
 *   tag_id    - The Exif Tag ID of the tag to be queried for.
 *   p_entry   - The pointer to the tag entry, filled by the function upon
 *               successful operation. Caller should not pre-allocate memory
 *               to hold the tag contents. Contents in the structure pointed
 *               by p_entry will be overwritten.
 * Return values:
 *     JPEGERR_SUCCESS
 *     JPEGERR_ENULLPTR
 *     JPEGERR_EFAILED
 *     JPEGERR_TAGABSENT
 * (See jpegerr.h for description of error values.)
 * Notes: none
 *****************************************************************************/
int exif_get_tag(exif_info_obj_t    obj,
                 exif_tag_id_t      tag_id,
                 exif_tag_entry_t  *p_entry);

/******************************************************************************
 * Function: exif_convert_tag_id_short_to_full
 * Description: The tag ID used in exif_set_tag and exif_get_tag are defined
 *              in exif.h as 32-bit ID's. The upper 16 bits are specially
 *              constructed to allow fast table lookup. The lower 16 bits are
 *              the same 16-bit tag ID defined by the EXIF specification.
 *              While one can use the defined constants directly in typical
 *              use where a known tag is to be retrieved or written, it is
 *              not possible to for example enumerate a list of tags in a
 *              for-loop given only the 16-bit tag IDs are known.
 *              For this reason, this function does the reverse lookup
 *              from the 16-bit IDs back to the 32-bit IDs.
 * Input parameters:
 *   short_tag - The 16-bit EXIF compliant ID.
 *   tag_id    - The 32-bit proprietary tag ID.
 * Return values:
 *     JPEGERR_SUCCESS
 *     JPEGERR_ENULLPTR
 *     JPEGERR_EFAILED
 * (See jpegerr.h for description of error values.)
 * Notes: none
 *****************************************************************************/
int exif_convert_tag_id_short_to_full(uint16_t short_tag,
                                      exif_tag_id_t *tag_id);

/******************************************************************************
 * Function: exif_list_tag_id
 * Description: List all the tags that are present in the exif info object.
 * Input parameters:
 *   obj            - The Exif Info object where the Exif tags should be obtained.
 *   p_tag_ids      - An array to hold the Exif Tag IDs to be retrieved. It should
 *                    be allocated by the caller.
 *   len            - The length of the array (the number of elements)
 *   p_len_required - Pointer to the holder to get back the required number of
 *                    elements enough to hold all tag ids present. It can be set
 *                    to NULL if it is not necessary.
 * Return values:
 *     JPEGERR_SUCCESS
 *     JPEGERR_ENULLPTR
 *     JPEGERR_EFAILED
 * (See jpegerr.h for description of error values.)
 * Notes: none
 *****************************************************************************/
int exif_list_tagid(exif_info_obj_t    obj,
                    exif_tag_id_t     *p_tag_ids,
                    uint32_t           len,
                    uint32_t          *p_len_required);

/******************************************************************************
 * Function: exif_destroy
 * Description: Releases all allocated memory made over the lifetime of the
 *              Exif Info object. One should always call this function to clean
 *              up an 'exif_init'-ed Exif Info object.
 * Input parameters:
 *   p_obj - The pointer to the Exif Info object to be destroyed.
 * Return values: None
 * Notes: none
 *****************************************************************************/
void exif_destroy(exif_info_obj_t *p_obj);

/* =======================================================================
**                          Macro Definitions
** ======================================================================= */
/* Enum defined to let compiler generate unique offset numbers for different
 * tags - ordering matters! NOT INTENDED to be used by any application. */
typedef enum
{
    // GPS IFD
    GPS_VERSION_ID = 0,
    GPS_LATITUDE_REF,
    GPS_LATITUDE,
    GPS_LONGITUDE_REF,
    GPS_LONGITUDE,
    GPS_ALTITUDE_REF,
    GPS_ALTITUDE,
    GPS_TIMESTAMP,
    GPS_SATELLITES,
    GPS_STATUS,
    GPS_MEASUREMODE,
    GPS_DOP,
    GPS_SPEED_REF,
    GPS_SPEED,
    GPS_TRACK_REF,
    GPS_TRACK,
    GPS_IMGDIRECTION_REF,
    GPS_IMGDIRECTION,
    GPS_MAPDATUM,
    GPS_DESTLATITUDE_REF,
    GPS_DESTLATITUDE,
    GPS_DESTLONGITUDE_REF,
    GPS_DESTLONGITUDE,
    GPS_DESTBEARING_REF,
    GPS_DESTBEARING,
    GPS_DESTDISTANCE_REF,
    GPS_DESTDISTANCE,
    GPS_PROCESSINGMETHOD,
    GPS_AREAINFORMATION,
    GPS_DATESTAMP,
    GPS_DIFFERENTIAL,

    // TIFF IFD
    COMPRESSION,
    IMAGE_DESCRIPTION,
    MAKE,
    MODEL,
    ORIENTATION,
    X_RESOLUTION,
    Y_RESOLUTION,
    RESOLUTION_UNIT,
    SOFTWARE,
    YCBCR_POSITIONING,

    // TIFF IFD (Thumbnail)
    TN_COMPRESSION,
    TN_IMAGE_DESCRIPTION,
    TN_MAKE,
    TN_MODEL,
    TN_ORIENTATION,
    TN_X_RESOLUTION,
    TN_Y_RESOLUTION,
    TN_RESOLUTION_UNIT,
    TN_SOFTWARE,
    TN_JPEGINTERCHANGE_FORMAT,
    TN_JPEGINTERCHANGE_FORMAT_L,
    TN_YCBCR_POSITIONING,

    // EXIF IFD
    EXPOSURE_TIME,
    F_NUMBER,
    EXPOSURE_PROGRAM,
    ISO_SPEED_RATING,
    OECF,
    EXIF_VERSION,
    EXIF_DATE_TIME_ORIGINAL,
    EXIF_DATE_TIME_DIGITIZED,
    EXIF_COMPONENTS_CONFIG,
    SHUTTER_SPEED,
    APERTURE,
    BRIGHTNESS,
    MAX_APERTURE,
    SUBJECT_DISTANCE,
    METERING_MODE,
    LIGHT_SOURCE,
    FLASH,
    FOCAL_LENGTH,
    EXIF_USER_COMMENT,
    EXIF_MAKER_NOTE,
    EXIF_FLASHPIX_VERSION,
    EXIF_COLOR_SPACE,
    EXIF_PIXEL_X_DIMENSION,
    EXIF_PIXEL_Y_DIMENSION,
    SUBJECT_LOCATION,
    EXPOSURE_INDEX,
    SENSING_METHOD,
    FILE_SOURCE,
    SCENE_TYPE,
    CFA_PATTERN,
    CUSTOM_RENDERED,
    EXPOSURE_MODE,
    WHITE_BALANCE,
    DIGITAL_ZOOM_RATIO,
    FOCAL_LENGTH_35MM,
    SCENE_CAPTURE_TYPE,
    GAIN_CONTROL,
    CONTRAST,
    SATURATION,
    SHARPNESS,
    SUBJECT_DISTANCE_RANGE,
    PIM,

    EXIF_TAG_MAX_OFFSET

} exif_tag_offset_t;

/* Below are the supported Tags (ID and structure for their data) */
#define CONSTRUCT_TAGID(offset,ID)   (offset << 16 | ID)

// GPS tag version
// Use EXIFTAGTYPE_GPS_VERSION_ID as the exif_tag_type (EXIF_BYTE)
// Count should be 4
#define _ID_GPS_VERSION_ID                  0x0000
#define EXIFTAGID_GPS_VERSION_ID            CONSTRUCT_TAGID(GPS_VERSION_ID, _ID_GPS_VERSION_ID)
#define EXIFTAGTYPE_GPS_VERSION_ID          EXIF_BYTE
// North or South Latitude
// Use EXIFTAGTYPE_GPS_LATITUDE_REF as the exif_tag_type (EXIF_ASCII)
// It should be 2 characters long including the null-terminating character.
#define _ID_GPS_LATITUDE_REF                0x0001
#define EXIFTAGID_GPS_LATITUDE_REF          CONSTRUCT_TAGID(GPS_LATITUDE_REF, _ID_GPS_LATITUDE_REF)
#define EXIFTAGTYPE_GPS_LATITUDE_REF         EXIF_ASCII
// Latitude
// Use EXIFTAGTYPE_GPS_LATITUDE as the exif_tag_type (EXIF_RATIONAL)
// Count should be 3
#define _ID_GPS_LATITUDE                    0x0002
#define EXIFTAGID_GPS_LATITUDE              CONSTRUCT_TAGID(GPS_LATITUDE, _ID_GPS_LATITUDE)
#define EXIFTAGTYPE_GPS_LATITUDE             EXIF_RATIONAL
// East or West Longitude
// Use EXIFTAGTYPE_GPS_LONGITUDE_REF as the exif_tag_type (EXIF_ASCII)
// It should be 2 characters long including the null-terminating character.
#define _ID_GPS_LONGITUDE_REF               0x0003
#define EXIFTAGID_GPS_LONGITUDE_REF         CONSTRUCT_TAGID(GPS_LONGITUDE_REF, _ID_GPS_LONGITUDE_REF)
#define EXIFTAGTYPE_GPS_LONGITUDE_REF       EXIF_ASCII
// Longitude
// Use EXIFTAGTYPE_GPS_LONGITUDE as the exif_tag_type (EXIF_RATIONAL)
// Count should be 3
#define _ID_GPS_LONGITUDE                   0x0004
#define EXIFTAGID_GPS_LONGITUDE             CONSTRUCT_TAGID(GPS_LONGITUDE, _ID_GPS_LONGITUDE)
#define EXIFTAGTYPE_GPS_LONGITUDE           EXIF_RATIONAL
// Altitude reference
// Use EXIFTAGTYPE_GPS_ALTITUDE_REF as the exif_tag_type (EXIF_BYTE)
#define _ID_GPS_ALTITUDE_REF                0x0005
#define EXIFTAGID_GPS_ALTITUDE_REF          CONSTRUCT_TAGID(GPS_ALTITUDE_REF, _ID_GPS_ALTITUDE_REF)
#define EXIFTAGTYPE_GPS_ALTITUDE_REF        EXIF_BYTE
// Altitude
// Use EXIFTAGTYPE_GPS_ALTITUDE as the exif_tag_type (EXIF_RATIONAL)
#define _ID_GPS_ALTITUDE                    0x0006
#define EXIFTAGID_GPS_ALTITUDE              CONSTRUCT_TAGID(GPS_ALTITUDE, _ID_GPS_ALTITUDE)
#define EXIFTAGTYPE_GPS_ALTITUE             EXIF_RATIONAL
// GPS time (atomic clock)
// Use EXIFTAGTYPE_GPS_TIMESTAMP as the exif_tag_type (EXIF_RATIONAL)
// Count should be 3
#define _ID_GPS_TIMESTAMP                   0x0007
#define EXIFTAGID_GPS_TIMESTAMP             CONSTRUCT_TAGID(GPS_TIMESTAMP, _ID_GPS_TIMESTAMP)
#define EXIFTAGTYPE_GPS_TIMESTAMP           EXIF_RATIONAL
// GPS Satellites
// Use EXIFTAGTYPE_GPS_SATELLITES as the exif_tag_type (EXIF_ASCII)
// Count can be anything.
#define _ID_GPS_SATELLITES                  0x0008
#define EXIFTAGID_GPS_SATELLITES            CONSTRUCT_TAGID(GPS_SATELLITES, _ID_GPS_SATELLITES)
#define EXIFTAGTYPE_GPS_SATELLITES          EXIF_ASCII
// GPS Status
// Use EXIFTAGTYPE_GPS_STATUS as the exif_tag_type (EXIF_ASCII)
// It should be 2 characters long including the null-terminating character.
// "A" - Measurement in progress
// "V" - Measurement Interoperability
// Other - Reserved
#define _ID_GPS_STATUS                      0x0009
#define EXIFTAGID_GPS_STATUS                CONSTRUCT_TAGID(GPS_STATUS, _ID_GPS_STATUS)
#define EXIFTATTYPE_GPS_STATUS              EXIF_ASCII
// GPS Measure Mode
// Use EXIFTAGTYPE_GPS_MEASUREMODE as the exif_tag_type (EXIF_ASCII)
// It should be 2 characters long including the null-terminating character.
// "2" - 2-dimensional measurement
// "3" - 3-dimensional measurement
// Other - Reserved
#define _ID_GPS_MEASUREMODE                 0x000a
#define EXIFTAGID_GPS_MEASUREMODE           CONSTRUCT_TAGID(GPS_MEASUREMODE, _ID_GPS_MEASUREMODE)
#define EXIFTAGTYPE_GPS_MEASUREMODE         EXIF_ASCII
// GPS Measurement precision (DOP)
// Use EXIFTAGTYPE_GPS_DOP as the exif_tag_type (EXIF_RATIONAL)
// Count should be 1
#define _ID_GPS_DOP                         0x000b
#define EXIFTAGID_GPS_DOP                   CONSTRUCT_TAGID(GPS_DOP, _ID_GPS_DOP)
#define EXIFTAGTYPE_GPS_DOP                 EXIF_RATIONAL
// Speed Unit
// Use EXIFTAGTYPE_GPS_SPEED_REF as the exif_tag_type (EXIF_ASCII)
// It should be 2 characters long including the null-terminating character.
// "K" - Kilometers per hour
// "M" - Miles per hour
// "N" - Knots
// Other - Reserved
#define _ID_GPS_SPEED_REF                   0x000c
#define EXIFTAGID_GPS_SPEED_REF             CONSTRUCT_TAGID(GPS_SPEED_REF, _ID_GPS_SPEED_REF)
#define EXIFTAGTYPE_GPS_SPEED_REF           EXIF_ASCII
// Speed of GPS receiver
// Use EXIFTAGTYPE_GPS_SPEED as the exif_tag_type (EXIF_RATIONAL)
// Count should be 1
#define _ID_GPS_SPEED                       0x000d
#define EXIFTAGID_GPS_SPEED                 CONSTRUCT_TAGID(GPS_SPEED, _ID_GPS_SPEED)
#define EXIFTAGTYPE_GPS_SPEED               EXIF_RATIONAL
// Reference of direction of movement
// Use EXIFTAGTYPE_GPS_TRACK_REF as the exif_tag_type (EXIF_ASCII)
// It should be 2 characters long including the null-terminating character.
// "T" - True direction
// "M" - Magnetic direction
// Other - Reserved
#define _ID_GPS_TRACK_REF                    0x000e
#define EXIFTAGID_GPS_TRACK_REF              CONSTRUCT_TAGID(GPS_TRACK_REF, _ID_GPS_TRACK_REF)
#define EXIFTAGTYPE_GPS_TRACK_REF            EXIF_ASCII
// Direction of movement
// Use EXIFTAGTYPE_GPS_TRACK as the exif_tag_type (EXIF_RATIONAL)
// Count should be 1
#define _ID_GPS_TRACK                       0x000f
#define EXIFTAGID_GPS_TRACK                 CONSTRUCT_TAGID(GPS_TRACK, _ID_GPS_TRACK)
#define EXIFTAGTYPE_GPS_TRACK               EXIF_RATIONAL
// Reference of direction of image
// Use EXIFTAGTYPE_GPS_IMGDIRECTION_REF as the exif_tag_type (EXIF_ASCII)
// It should be 2 characters long including the null-terminating character.
// "T" - True direction
// "M" - Magnetic direction
// Other - Reserved
#define _ID_GPS_IMGDIRECTION_REF            0x0010
#define EXIFTAGID_GPS_IMGDIRECTION_REF      CONSTRUCT_TAGID(GPS_IMGDIRECTION_REF, _ID_GPS_IMGDIRECTION_REF)
#define EXIFTAGTYPE_GPS_IMGDIRECTION_REF    EXIF_ASCII
// Direction of image
// Use EXIFTAGTYPE_GPS_IMGDIRECTION as the exif_tag_type (EXIF_RATIONAL)
// Count should be 1
#define _ID_GPS_IMGDIRECTION                0x0011
#define EXIFTAGID_GPS_IMGDIRECTION          CONSTRUCT_TAGID(GPS_IMGDIRECTION, _ID_GPS_IMGDIRECTION)
#define EXIFTAGTYPE_GPS_IMGDIRECTION        EXIF_RATIONAL
// Geodetic survey data used
// Use EXIFTAGTYPE_GPS_MAPDATUM as the exif_tag_type (EXIF_ASCII)
// Count can be any
#define _ID_GPS_MAPDATUM                    0x0012
#define EXIFTAGID_GPS_MAPDATUM              CONSTRUCT_TAGID(GPS_MAPDATUM, _ID_GPS_MAPDATUM)
#define EXIFTAGTYPE_GPS_MAPDATUM            EXIF_ASCII
// Reference for latitude of destination
// Use EXIFTAGTYPE_GPS_DESTLATITUDE_REF as the exif_tag_type (EXIF_ASCII)
// It should be 2 characters long including the null-terminating character.
// "N" - North latitude
// "S" - South latitude
// Other - Reserved
#define _ID_GPS_DESTLATITUDE_REF            0x0013
#define EXIFTAGID_GPS_DESTLATITUDE_REF      CONSTRUCT_TAGID(GPS_DESTLATITUDE_REF, _ID_GPS_DESTLATITUDE_REF)
#define EXIFTAGTYPE_GPS_DESTLATITUDE_REF    EXIF_ASCII
// Latitude of destination
// Use EXIFTAGTYPE_GPS_DESTLATITUDE as the exif_tag_type (EXIF_RATIONAL)
// Count should be 3
#define _ID_GPS_DESTLATITUDE                0x0014
#define EXIFTAGID_GPS_DESTLATITUDE          CONSTRUCT_TAGID(GPS_DESTLATITUDE, _ID_GPS_DESTLATITUDE)
#define EXIFTAGTYPE_GPS_DESTLATITUDE        EXIF_RATIONAL
// Reference for longitude of destination
// Use EXIFTAGTYPE_GPS_DESTLONGITUDE_REF as the exif_tag_type (EXIF_ASCII)
// It should be 2 characters long including the null-terminating character.
// "E" - East longitude
// "W" - West longitude
// Other - Reserved
#define _ID_GPS_DESTLONGITUDE_REF           0x0015
#define EXIFTAGID_GPS_DESTLONGITUDE_REF     CONSTRUCT_TAGID(GPS_DESTLONGITUDE_REF, _ID_GPS_DESTLONGITUDE_REF)
#define EXIFTAGTYPE_GPS_DESTLONGITUDE_REF   EXIF_ASCII
// Longitude of destination
// Use EXIFTAGTYPE_GPS_DESTLONGITUDE as the exif_tag_type (EXIF_RATIONAL)
// Count should be 3
#define _ID_GPS_DESTLONGITUDE               0x0016
#define EXIFTAGID_GPS_DESTLONGITUDE         CONSTRUCT_TAGID(GPS_DESTLONGITUDE, _ID_GPS_DESTLONGITUDE)
#define EXIFTAGTYPE_GPS_DESTLONGITUDE       EXIF_RATIONAL
// Reference for bearing of destination
// Use EXIFTAGTYPE_GPS_DESTBEARING_REF as the exif_tag_type (EXIF_ASCII)
// It should be 2 characters long including the null-terminating character.
// "T" - True direction
// "M" - Magnetic direction
// Other - Reserved
#define _ID_GPS_DESTBEARING_REF             0x0017
#define EXIFTAGID_GPS_DESTBEARING_REF       CONSTRUCT_TAGID(GPS_DESTBEARING_REF, _ID_GPS_DESTBEARING_REF)
#define EXIFTAGTYPE_GPS_DESTBEARING_REF     EXIF_ASCII
// Bearing of destination
// Use EXIFTAGTYPE_GPS_DESTBEARING as the exif_tag_type (EXIF_RATIONAL)
// Count should be 1
#define _ID_GPS_DESTBEARING                 0x0018
#define EXIFTAGID_GPS_DESTBEARING           CONSTRUCT_TAGID(GPS_DESTBEARING, _ID_GPS_DESTBEARING)
#define EXIFTAGTYPE_GPS_DESTBEARING         EXIF_RATIONAL
// Reference for distance to destination
// Use EXIFTAGTYPE_GPS_DESTDISTANCE_REF as the exif_tag_type (EXIF_ASCII)
// It should be 2 characters long including the null-terminating character.
// "K" - Kilometers per hour
// "M" - Miles per hour
// "N" - Knots
// Other - Reserved
#define _ID_GPS_DESTDISTANCE_REF            0x0019
#define EXIFTAGID_GPS_DESTDISTANCE_REF      CONSTRUCT_TAGID(GPS_DESTDISTANCE_REF, _ID_GPS_DESTDISTANCE_REF)
#define EXIFTAGTYPE_GPS_DESTDISTANCE_REF    EXIF_ASCII
// Distance to destination
// Use EXIFTAGTYPE_GPS_DESTDISTANCE as the exif_tag_type (EXIF_RATIONAL)
// Count should be 1
#define _ID_GPS_DESTDISTANCE                0x001a
#define EXIFTAGID_GPS_DESTDISTANCE          CONSTRUCT_TAGID(GPS_DESTDISTANCE, _ID_GPS_DESTDISTANCE)
#define EXIFTAGTYPE_GPS_DESTDISTANCE        EXIF_RATIONAL
// Name of GPS processing method
// Use EXIFTAGTYPE_GPS_PROCESSINGMETHOD as the exif_tag_type (EXIF_UNDEFINED)
// Count can be any
#define _ID_GPS_PROCESSINGMETHOD            0x001b
#define EXIFTAGID_GPS_PROCESSINGMETHOD      CONSTRUCT_TAGID(GPS_PROCESSINGMETHOD, _ID_GPS_PROCESSINGMETHOD)
#define EXIFTAGTYPE_GPS_PROCESSINGMETHOD    EXIF_UNDEFINED
// Name of GPS area
// Use EXIFTAGTYPE_GPS_AREAINFORMATION as the exif_tag_type (EXIF_UNDEFINED)
// Count can be any
#define _ID_GPS_AREAINFORMATION             0x001c
#define EXIFTAGID_GPS_AREAINFORMATION       CONSTRUCT_TAGID(GPS_AREAINFORMATION, _ID_GPS_AREAINFORMATION)
#define EXIFTAGTYPE_GPS_AREAINFORMATION     EXIF_UNDEFINED
// GPS date
// Use EXIFTAGTYPE_GPS_DATESTAMP as the exif_tag_type (EXIF_ASCII)
// It should be 11 characters long including the null-terminating character.
#define _ID_GPS_DATESTAMP                   0x001d
#define EXIFTAGID_GPS_DATESTAMP             CONSTRUCT_TAGID(GPS_DATESTAMP, _ID_GPS_DATESTAMP)
#define EXIFTAGTYPE_GPS_DATESTAMP           EXIF_ASCII
// GPS differential correction
// Use EXIFTAGTYPE_GPS_DIFFERENTIAL as the exif_tag_type (EXIF_SHORT)
// Count should be 1
// 0 - Measurement without differential correction
// 1 - Differential correction applied
// Other - Reserved
#define _ID_GPS_DIFFERENTIAL                0x001e
#define EXIFTAGID_GPS_DIFFERENTIAL          CONSTRUCT_TAGID(GPS_DIFFERENTIAL, _ID_GPS_DIFFERENTIAL)
#define EXIFTAGTYPE_GPS_DIFFERENTIAL        EXIF_SHORT

// Compression scheme
// Use EXIFTAGTYPE_COMPRESSION as the exif_tag_type (EXIF_SHORT)
// Count should be 1
#define _ID_COMPRESSION                     0x0103
#define EXIFTAGID_COMPRESSION               CONSTRUCT_TAGID(COMPRESSION, _ID_COMPRESSION)
#define EXIFTAGTYPE_COMPRESSION             EXIF_SHORT
// Image title
// Use EXIFTAGTYPE_IMAGE_DESCRIPTION as the exif_tag_type (EXIF_ASCII)
// Count can be any
#define _ID_IMAGE_DESCRIPTION               0x010e
#define EXIFTAGID_IMAGE_DESCRIPTION         CONSTRUCT_TAGID(IMAGE_DESCRIPTION, _ID_IMAGE_DESCRIPTION)
#define EXIFTAGTYPE_IMAGE_DESCRIPTION       EXIF_ASCII
// Image input equipment manufacturer
// Use EXIFTAGTYPE_MAKE as the exif_tag_type (EXIF_ASCII)
// Count can be any
#define _ID_MAKE                            0x010f
#define EXIFTAGID_MAKE                      CONSTRUCT_TAGID(MAKE, _ID_MAKE)
#define EXIFTAGTYPE_MAKE                    EXIF_ASCII
// Image input equipment model
// Use EXIFTAGTYPE_MODEL as the exif_tag_type (EXIF_ASCII)
// Count can be any
#define _ID_MODEL                           0x0110
#define EXIFTAGID_MODEL                     CONSTRUCT_TAGID(MODEL, _ID_MODEL)
#define EXIFTAGTYPE_MODEL                   EXIF_ASCII
// Orientation of image
// Use EXIFTAGTYPE_ORIENTATION as the exif_tag_type (EXIF_SHORT)
// Count should be 1
#define _ID_ORIENTATION                     0x0112
#define EXIFTAGID_ORIENTATION               CONSTRUCT_TAGID(ORIENTATION, _ID_ORIENTATION)
#define EXIFTAGTYPE_ORIENTATION             EXIF_SHORT
// Image resolution in width direction
// Use EXIFTAGTYPE_X_RESOLUTION as the exif_tag_type (EXIF_RATIONAL)
// Count should be 1
#define _ID_X_RESOLUTION                    0x011a
#define EXIFTAGID_X_RESOLUTION              CONSTRUCT_TAGID(X_RESOLUTION, _ID_X_RESOLUTION)
#define EXIFTAGTYPE_X_RESOLUTION            EXIF_RATIONAL
// Image resolution in height direction
// Use EXIFTAGTYPE_Y_RESOLUTION as the exif_tag_type (EXIF_RATIONAL)
// Count should be 1
#define _ID_Y_RESOLUTION                    0x011b
#define EXIFTAGID_Y_RESOLUTION              CONSTRUCT_TAGID(Y_RESOLUTION, _ID_Y_RESOLUTION)
#define EXIFTAGTYPE_Y_RESOLUTION            EXIF_RATIONAL
// Unit of X and Y resolution
// Use EXIFTAGTYPE_RESOLUTION_UNIT as the exif_tag_type (EXIF_SHORT)
// Count should be 1
#define _ID_RESOLUTION_UNIT                 0x0128
#define EXIFTAGID_RESOLUTION_UNIT           CONSTRUCT_TAGID(RESOLUTION_UNIT, _ID_RESOLUTION_UNIT)
#define EXIFTAGTYPE_RESOLUTION_UNIT         EXIF_SHORT
// Software used
// Use EXIFTAGTYPE_SOFTWARE as the exif_tag_type (EXIF_ASCII)
// Count can be any
#define _ID_SOFTWARE                        0x0131
#define EXIFTAGID_SOFTWARE                  CONSTRUCT_TAGID(SOFTWARE, _ID_SOFTWARE)
#define EXIFTAGTYPE_SOFTWARE                EXIF_ASCII
// Y and C positioning
// Use EXIFTAGTYPE_YCBCR_POSITIONING as the exif_tag_type (EXIF_SHORT)
// Count should be 1
#define _ID_YCBCR_POSITIONING               0x0213
#define EXIFTAGID_YCBCR_POSITIONING         CONSTRUCT_TAGID(YCBCR_POSITIONING, _ID_YCBCR_POSITIONING)
#define EXIFTAGTYPE_YCBCR_POSITIONING       EXIF_SHORT
// Compression scheme (of thumbnail)
// Use EXIFTAGTYPE_TN_COMPRESSION as the exif_tag_type (EXIF_SHORT)
// Count should be 1
#define _ID_TN_COMPRESSION                  0x0103
#define EXIFTAGID_TN_COMPRESSION            CONSTRUCT_TAGID(TN_COMPRESSION, _ID_TN_COMPRESSION)
#define EXIFTAGTYPE_TN_COMPRESSION          EXIF_SHORT
// Image title (of thumbnail)
// Use EXIFTAGTYPE_TN_IMAGE_DESCRIPTION as the exif_tag_type (EXIF_ASCII)
// Count can be any
#define _ID_TN_IMAGE_DESCRIPTION            0x010e
#define EXIFTAGID_TN_IMAGE_DESCRIPTION      CONSTRUCT_TAGID(TN_IMAGE_DESCRIPTION, _ID_TN_IMAGE_DESCRIPTION)
#define EXIFTAGTYPE_TN_IMAGE_DESCRIPTION    EXIF_ASCII
// Image input equipment manufacturer (of thumbnail)
// Use EXIFTAGTYPE_TN_MAKE as the exif_tag_type (EXIF_ASCII)
// Count can be any
#define _ID_TN_MAKE                         0x010f
#define EXIFTAGID_TN_MAKE                   CONSTRUCT_TAGID(TN_MAKE, _ID_TN_MAKE)
#define EXIFTAGTYPE_TN_MAKE                 EXIF_ASCII
// Image input equipment model (of thumbnail)
// Use EXIFTAGTYPE_TN_MODEL as the exif_tag_type (EXIF_ASCII)
// Count can be any
#define _ID_TN_MODEL                        0x0110
#define EXIFTAGID_TN_MODEL                  CONSTRUCT_TAGID(TN_MODEL, _ID_TN_MODEL)
#define EXIFTAGTYPE_TN_MODEL                EXIF_ASCII
// Orientation of image (of thumbnail)
// Use EXIFTAGTYPE_TN_ORIENTATION as the exif_tag_type (EXIF_SHORT)
// Count should be 1
#define _ID_TN_ORIENTATION                  0x0112
#define EXIFTAGID_TN_ORIENTATION            CONSTRUCT_TAGID(TN_ORIENTATION, _ID_TN_ORIENTATION)
#define EXIFTAGTYPE_TN_ORIENTATION          EXIF_SHORT
// Image resolution in width direction (of thumbnail)
// Use EXIFTAGTYPE_TN_X_RESOLUTION as the exif_tag_type (EXIF_RATIONAL)
// Count should be 1
#define _ID_TN_X_RESOLUTION                 0x011a
#define EXIFTAGID_TN_X_RESOLUTION           CONSTRUCT_TAGID(TN_X_RESOLUTION, _ID_TN_X_RESOLUTION)
#define EXIFTAGTYPE_TN_X_RESOLUTION         EXIF_RATIONAL
// Image resolution in height direction  (of thumbnail)
// Use EXIFTAGTYPE_TN_Y_RESOLUTION as the exif_tag_type (EXIF_RATIONAL)
// Count should be 1
#define _ID_TN_Y_RESOLUTION                 0x011b
#define EXIFTAGID_TN_Y_RESOLUTION           CONSTRUCT_TAGID(TN_Y_RESOLUTION, _ID_TN_Y_RESOLUTION)
#define EXIFTAGTYPE_TN_Y_RESOLUTION         EXIF_RATIONAL
// Unit of X and Y resolution (of thumbnail)
// Use EXIFTAGTYPE_TN_RESOLUTION_UNIT as the exif_tag_type (EXIF_SHORT)
// Count should be 1
#define _ID_TN_RESOLUTION_UNIT              0x128
#define EXIFTAGID_TN_RESOLUTION_UNIT        CONSTRUCT_TAGID(TN_RESOLUTION_UNIT, _ID_TN_RESOLUTION_UNIT)
#define EXIFTAGTYPE_TN_RESOLUTION_UNIT      EXIF_SHORT
// Software used (of thumbnail)
// Use EXIFTAGTYPE_TN_SOFTWARE as the exif_tag_type (EXIF_ASCII)
// Count can be any
#define _ID_TN_SOFTWARE                     0x0131
#define EXIFTAGID_TN_SOFTWARE               CONSTRUCT_TAGID(TN_SOFTWARE, _ID_TN_SOFTWARE)
#define EXIFTAGTYPE_TN_SOFTWARE             EXIF_ASCII
/// Offset to JPEG SOI (of thumbnail)
#define _ID_TN_JPEGINTERCHANGE_FORMAT            0x0201
#define EXIFTAGID_TN_JPEGINTERCHANGE_FORMAT      CONSTRUCT_TAGID(TN_JPEGINTERCHANGE_FORMAT, _ID_TN_JPEGINTERCHANGE_FORMAT)
#define EXIFTAGTYPE_TN_JPEGINTERCHANGE_FORMAT    EXIF_LONG
/// Bytes of JPEG data (of thumbnail)
#define _ID_TN_JPEGINTERCHANGE_FORMAT_L          0x0202
#define EXIFTAGID_TN_JPEGINTERCHANGE_FORMAT_L    CONSTRUCT_TAGID(TN_JPEGINTERCHANGE_FORMAT_L, _ID_TN_JPEGINTERCHANGE_FORMAT_L)
#define EXIFTAGTYPE_TN_JPEGINTERCHANGE_FORMAT_T  EXIF_LONG
// Y and C positioning (of thumbnail)
// Use EXIFTAGTYPE_TN_YCBCR_POSITIONING as the exif_tag_type (EXIF_SHORT)
// Count should be 1
#define _ID_TN_YCBCR_POSITIONING            0x0213
#define EXIFTAGID_TN_YCBCR_POSITIONING      CONSTRUCT_TAGID(TN_YCBCR_POSITIONING, _ID_TN_YCBCR_POSITIONING)
#define EXIFTAGTYPE_TN_YCBCR_POSITIONING    EXIF_SHORT
// Exposure time
// Use EXIFTAGTYPE_EXPOSURE_TIME as the exif_tag_type (EXIF_RATIONAL)
// Count should be 1
#define _ID_EXPOSURE_TIME                   0x829a
#define EXIFTAGID_EXPOSURE_TIME             CONSTRUCT_TAGID(EXPOSURE_TIME, _ID_EXPOSURE_TIME)
#define EXIFTAGTYPE_EXPOSURE_TIME           EXIF_RATIONAL
// F number
// Use EXIFTAGTYPE_F_NUMBER as the exif_tag_type (EXIF_RATIONAL)
// Count should be 1
#define _ID_F_NUMBER                        0x829d
#define EXIFTAGID_F_NUMBER                  CONSTRUCT_TAGID(F_NUMBER, _ID_F_NUMBER)
#define EXIFTAGTYPE_F_NUMBER                EXIF_RATIONAL
// Exif IFD pointer (NOT INTENDED to be accessible to user)
#define _ID_EXIF_IFD_PTR                    0x8769
#define EXIFTAGID_EXIF_IFD_PTR              CONSTRUCT_TAGID(EXIF_TAG_MAX_OFFSET, _ID_EXIF_IFD_PTR)
#define EXIFTAGTYPE_EXIF_IFD_PTR            EXIF_LONG
// Exposure program
// Use EXIFTAGTYPE_EXPOSURE_PROGRAM as the exif_tag_type (EXIF_SHORT)
// Count should be 1
#define _ID_EXPOSURE_PROGRAM                0x8822
#define EXIFTAGID_EXPOSURE_PROGRAM          CONSTRUCT_TAGID(EXPOSURE_PROGRAM, _ID_EXPOSURE_PROGRAM)
#define EXIFTAGTYPE_EXPOSURE_PROGRAM        EXIF_SHORT
// GPS IFD pointer (NOT INTENDED to be accessible to user)
#define _ID_GPS_IFD_PTR                     0x8825
#define EXIFTAGID_GPS_IFD_PTR               CONSTRUCT_TAGID(EXIF_TAG_MAX_OFFSET, _ID_GPS_IFD_PTR)
#define EXIFTAGTYPE_GPS_IFD_PTR             EXIF_LONG
// ISO Speed Rating
// Use EXIFTAGTYPE_ISO_SPEED_RATING as the exif_tag_type (EXIF_SHORT)
// Count can be any
#define _ID_ISO_SPEED_RATING                0x8827
#define EXIFTAGID_ISO_SPEED_RATING          CONSTRUCT_TAGID(ISO_SPEED_RATING, _ID_ISO_SPEED_RATING)
#define EXIFTAGTYPE_ISO_SPEED_RATING        EXIF_SHORT
// Optoelectric conversion factor
// Use EXIFTAGTYPE_OECF as the exif_tag_type (EXIF_UNDEFINED)
// Count can be any
#define _ID_OECF                           0x8828
#define EXIFTAGID_OECF                      CONSTRUCT_TAGID(OECF, _ID_OECF)
#define EXIFTAGTYPE_OECF                    EXIF_UNDEFINED
// Exif version
// Use EXIFTAGTYPE_EXIF_VERSION as the exif_tag_type (EXIF_UNDEFINED)
// Count should be 4
#define _ID_EXIF_VERSION                    0x9000
#define EXIFTAGID_EXIF_VERSION              CONSTRUCT_TAGID(EXIF_VERSION, _ID_EXIF_VERSION)
#define EXIFTAGTYPE_EXIF_VERSION            EXIF_UNDEFINED
// Date and time of original data gerneration
// Use EXIFTAGTYPE_EXIF_DATE_TIME_ORIGINAL as the exif_tag_type (EXIF_ASCII)
// It should be 20 characters long including the null-terminating character.
#define _ID_EXIF_DATE_TIME_ORIGINAL          0x9003
#define EXIFTAGID_EXIF_DATE_TIME_ORIGINAL    CONSTRUCT_TAGID(EXIF_DATE_TIME_ORIGINAL, _ID_EXIF_DATE_TIME_ORIGINAL)
#define EXIFTAGTYPE_EXIF_DATE_TIME_ORIGINAL  EXIF_ASCII
// Date and time of digital data generation
// Use EXIFTAGTYPE_EXIF_DATE_TIME_DIGITIZED as the exif_tag_type (EXIF_ASCII)
// It should be 20 characters long including the null-terminating character.
#define _ID_EXIF_DATE_TIME_DIGITIZED         0x9004
#define EXIFTAGID_EXIF_DATE_TIME_DIGITIZED   CONSTRUCT_TAGID(EXIF_DATE_TIME_DIGITIZED, _ID_EXIF_DATE_TIME_DIGITIZED)
#define EXIFTAGTYPE_EXIF_DATE_TIME_DIGITIZED EXIF_ASCII
// Meaning of each component
// Use EXIFTAGTYPE_EXIF_COMPONENTS_CONFIG as the exif_tag_type (EXIF_UNDEFINED)
// Count should be 4
#define _ID_EXIF_COMPONENTS_CONFIG          0x9101
#define EXIFTAGID_EXIF_COMPONENTS_CONFIG    CONSTRUCT_TAGID(EXIF_COMPONENTS_CONFIG, _ID_EXIF_COMPONENTS_CONFIG)
#define EXIFTAGTYPE_EXIF_COMPONENTS_CONFIG  EXIF_UNDEFINED
// Shutter speed
// Use EXIFTAGTYPE_SHUTTER_SPEED as the exif_tag_type (EXIF_SRATIONAL)
// Count should be 1
#define _ID_SHUTTER_SPEED                   0x9201
#define EXIFTAGID_SHUTTER_SPEED             CONSTRUCT_TAGID(SHUTTER_SPEED, _ID_SHUTTER_SPEED)
#define EXIFTAGTYPE_SHUTTER_SPEED           EXIF_SRATIONAL
// Aperture
// Use EXIFTAGTYPE_APERTURE as the exif_tag_type (EXIF_RATIONAL)
// Count should be 1
#define _ID_APERTURE                        0x9202
#define EXIFTAGID_APERTURE                  CONSTRUCT_TAGID(APERTURE, _ID_APERTURE)
#define EXIFTAGTYPE_APERTURE                EXIF_RATIONAL
// Brigthness
// Use EXIFTAGTYPE_BRIGHTNESS as the exif_tag_type (EXIF_SRATIONAL)
// Count should be 1
#define _ID_BRIGHTNESS                      0x9203
#define EXIFTAGID_BRIGHTNESS                CONSTRUCT_TAGID(BRIGHTNESS, _ID_BRIGHTNESS)
#define EXIFTAGTYPE_BRIGHTNESS              EXIF_SRATIONAL
// Maximum lens aperture
// Use EXIFTAGTYPE_MAX_APERTURE as the exif_tag_type (EXIF_RATIONAL)
// Count should be 1
#define _ID_MAX_APERTURE                    0x9205
#define EXIFTAGID_MAX_APERTURE              CONSTRUCT_TAGID(MAX_APERTURE, _ID_MAX_APERTURE)
#define EXIFTAGTYPE_MAX_APERTURE            EXIF_RATIONAL
// Subject distance
// Use EXIFTAGTYPE_SUBJECT_DISTANCE as the exif_tag_type (EXIF_RATIONAL)
// Count should be 1
#define _ID_SUBJECT_DISTANCE                0x9206
#define EXIFTAGID_SUBJECT_DISTANCE          CONSTRUCT_TAGID(SUBJECT_DISTANCE, _ID_SUBJECT_DISTANCE)
#define EXIFTAGTYPE_SUBJECT_DISTANCE        EXIF_RATIONAL
// Metering mode
// Use EXIFTAGTYPE_METERING_MODE as the exif_tag_type (EXIF_SHORT)
// Count should be 1
#define _ID_METERING_MODE                   0x9207
#define EXIFTAGID_METERING_MODE             CONSTRUCT_TAGID(METERING_MODE, _ID_METERING_MODE)
#define EXIFTAGTYPE_METERING_MODE           EXIF_SHORT
// Light source
// Use EXIFTAGTYPE_LIGHT_SOURCE as the exif_tag_type (EXIF_SHORT)
// Count should be 1
#define _ID_LIGHT_SOURCE                    0x9208
#define EXIFTAGID_LIGHT_SOURCE              CONSTRUCT_TAGID(LIGHT_SOURCE, _ID_LIGHT_SOURCE)
#define EXIFTAGTYPE_LIGHT_SOURCE            EXIF_SHORT
// Flash
// Use EXIFTAGTYPE_FLASH as the exif_tag_type (EXIF_SHORT)
// Count should be 1
#define _ID_FLASH                           0x9209
#define EXIFTAGID_FLASH                     CONSTRUCT_TAGID(FLASH, _ID_FLASH)
#define EXIFTAGTYPE_FLASH                   EXIF_SHORT
// Lens focal length
// Use EXIFTAGTYPE_FOCAL_LENGTH as the exif_tag_type (EXIF_RATIONAL)
// Count should be 1
#define _ID_FOCAL_LENGTH                    0x920a
#define EXIFTAGID_FOCAL_LENGTH              CONSTRUCT_TAGID(FOCAL_LENGTH, _ID_FOCAL_LENGTH)
#define EXIFTAGTYPE_FOCAL_LENGTH            EXIF_RATIONAL
// User comments
// Use EXIFTAGTYPE_EXIF_USER_COMMENT as the exif_tag_type (EXIF_UNDEFINED)
// Count can be any
#define _ID_EXIF_USER_COMMENT               0x9286
#define EXIFTAGID_EXIF_USER_COMMENT         CONSTRUCT_TAGID(EXIF_USER_COMMENT, _ID_EXIF_USER_COMMENT)
#define EXIFTAGTYPE_EXIF_USER_COMMENT       EXIF_UNDEFINED
// Maker note
// Use EXIFTAGTYPE_EXIF_MAKER_NOTE as the exif_tag_type (EXIF_UNDEFINED)
// Count can be any
#define _ID_EXIF_MAKER_NOTE                 0x927c
#define EXIFTAGID_EXIF_MAKER_NOTE           CONSTRUCT_TAGID(EXIF_MAKER_NOTE, _ID_EXIF_MAKER_NOTE)
#define EXIFTAGTYPE_EXIF_MAKER_NOTE         EXIF_UNDEFINED
// Supported Flashpix version
// Use EXIFTAGTYPE_EXIF_FLASHPIX_VERSION as the exif_tag_type (EXIF_UNDEFINED)
// Count should be 4
#define _ID_EXIF_FLASHPIX_VERSION           0xa000
#define EXIFTAGID_EXIF_FLASHPIX_VERSION     CONSTRUCT_TAGID(EXIF_FLASHPIX_VERSION, _ID_EXIF_FLASHPIX_VERSION)
#define EXIFTAGTYPE_EXIF_FLASHPIX_VERSION   EXIF_UNDEFINED
//  Color space information
// Use EXIFTAGTYPE_EXIF_COLOR_SPACE as the exif_tag_type (EXIF_SHORT)
// Count should be 1
#define _ID_EXIF_COLOR_SPACE                0xa001
#define EXIFTAGID_EXIF_COLOR_SPACE          CONSTRUCT_TAGID(EXIF_COLOR_SPACE, _ID_EXIF_COLOR_SPACE)
#define EXIFTAGTYPE_EXIF_COLOR_SPACE        EXIF_SHORT
//  Valid image width
// Use EXIFTAGTYPE_EXIF_PIXEL_X_DIMENSION as the exif_tag_type (EXIF_SHORT)
// Count should be 1
#define _ID_EXIF_PIXEL_X_DIMENSION          0xa002
#define EXIFTAGID_EXIF_PIXEL_X_DIMENSION    CONSTRUCT_TAGID(EXIF_PIXEL_X_DIMENSION, _ID_EXIF_PIXEL_X_DIMENSION)
#define EXIFTAGTYPE_EXIF_PIXEL_X_DIMENSION  EXIF_SHORT
// Valid image height
// Use EXIFTAGTYPE_EXIF_PIXEL_Y_DIMENSION as the exif_tag_type (EXIF_SHORT)
// Count should be 1
#define _ID_EXIF_PIXEL_Y_DIMENSION          0xa003
#define EXIFTAGID_EXIF_PIXEL_Y_DIMENSION    CONSTRUCT_TAGID(EXIF_PIXEL_Y_DIMENSION, _ID_EXIF_PIXEL_Y_DIMENSION)
#define EXIFTAGTYPE_EXIF_PIXEL_Y_DIMENSION  EXIF_SHORT
// Interop IFD pointer (NOT INTENDED to be accessible to user)
#define _ID_INTEROP_IFD_PTR                 0xa005
#define EXIFTAGID_INTEROP_IFD_PTR           CONSTRUCT_TAGID(EXIF_TAG_MAX_OFFSET, _ID_INTEROP_IFD_PTR)
#define EXIFTAGTYPE_INTEROP_IFD_PTR         EXIF_LONG
// Subject location
// Use EXIFTAGTYPE_SUBJECT_LOCATION as the exif_tag_type (EXIF_SHORT)
// Count should be 2
#define _ID_SUBJECT_LOCATION                0xa214
#define EXIFTAGID_SUBJECT_LOCATION          CONSTRUCT_TAGID(SUBJECT_LOCATION, _ID_SUBJECT_LOCATION)
#define EXIFTAGTYPE_SUBJECT_LOCATION        EXIF_SHORT
// Exposure index
// Use EXIFTAGTYPE_EXPOSURE_INDEX as the exif_tag_type (EXIF_RATIONAL)
// Count should be 1
#define _ID_EXPOSURE_INDEX                  0xa215
#define EXIFTAGID_EXPOSURE_INDEX            CONSTRUCT_TAGID(EXPOSURE_INDEX, _ID_EXPOSURE_INDEX)
#define EXIFTAGTYPE_EXPOSURE_INDEX          EXIF_RATIONAL
// Sensing method
// Use EXIFTAGTYPE_SENSING_METHOD as the exif_tag_type (EXIF_SHORT)
// Count should be 1
#define _ID_SENSING_METHOD                  0xa217
#define EXIFTAGID_SENSING_METHOD            CONSTRUCT_TAGID(SENSING_METHOD, _ID_SENSING_METHOD)
#define EXIFTAGTYPE_SENSING_METHOD          EXIF_SHORT
// File source
// Use EXIFTAGTYPE_FILE_SOURCE as the exif_tag_type (EXIF_UNDEFINED)
// Count should be 1
#define _ID_FILE_SOURCE                     0xa300
#define EXIFTAGID_FILE_SOURCE               CONSTRUCT_TAGID(FILE_SOURCE, _ID_FILE_SOURCE)
#define EXIFTAGTYPE_FILE_SOURCE             EXIF_UNDEFINED
// Scene type
// Use EXIFTAGTYPE_SCENE_TYPE as the exif_tag_type (EXIF_UNDEFINED)
// Count should be 1
#define _ID_SCENE_TYPE                      0xa301
#define EXIFTAGID_SCENE_TYPE                CONSTRUCT_TAGID(SCENE_TYPE, _ID_SCENE_TYPE)
#define EXIFTAGTYPE_SCENE_TYPE              EXIF_UNDEFINED
// CFA pattern
// Use EXIFTAGTYPE_CFA_PATTERN as the exif_tag_type (EXIF_UNDEFINED)
// Count can be any
#define _ID_CFA_PATTERN                     0xa302
#define EXIFTAGID_CFA_PATTERN               CONSTRUCT_TAGID(CFA_PATTERN, _ID_CFA_PATTERN)
#define EXIFTAGTYPE_CFA_PATTERN             EXIF_UNDEFINED
// Custom image processing
// Use EXIFTAGTYPE_CUSTOM_RENDERED as the exif_tag_type (EXIF_SHORT)
// Count should be 1
#define _ID_CUSTOM_RENDERED                 0xa401
#define EXIFTAGID_CUSTOM_RENDERED           CONSTRUCT_TAGID(CUSTOM_RENDERED, _ID_CUSTOM_RENDERED)
#define EXIFTAGTYPE_CUSTOM_RENDERED         EXIF_SHORT
// Exposure mode
// Use EXIFTAGTYPE_EXPOSURE_MODE as the exif_tag_type (EXIF_SHORT)
// Count should be 1
#define _ID_EXPOSURE_MODE                   0xa402
#define EXIFTAGID_EXPOSURE_MODE             CONSTRUCT_TAGID(EXPOSURE_MODE, _ID_EXPOSURE_MODE)
#define EXIFTAGTYPE_EXPOSURE_MODE           EXIF_SHORT
// White balance
// Use EXIFTAGTYPE_WHITE_BALANCE as the exif_tag_type (EXIF_SHORT)
// Count should be 1
#define _ID_WHITE_BALANCE                   0xa403
#define EXIFTAGID_WHITE_BALANCE             CONSTRUCT_TAGID(WHITE_BALANCE, _ID_WHITE_BALANCE)
#define EXIFTAGTYPE_WHITE_BALANCE           EXIF_SHORT
// Digital zoom ratio
// Use EXIFTAGTYPE_DIGITAL_ZOOM_RATIO as the exif_tag_type (EXIF_RATIONAL)
// Count should be 1
#define _ID_DIGITAL_ZOOM_RATIO              0xa404
#define EXIFTAGID_DIGITAL_ZOOM_RATIO        CONSTRUCT_TAGID(DIGITAL_ZOOM_RATIO, _ID_DIGITAL_ZOOM_RATIO)
#define EXIFTAGTYPE_DIGITAL_ZOOM_RATIO      EXIF_RATIONAL
// Focal length in 35mm film
// Use EXIFTAGTYPE_FOCAL_LENGTH_35MM as the exif_tag_type (EXIF_SHORT)
// Count should be 1
#define _ID_FOCAL_LENGTH_35MM               0xa405
#define EXIFTAGID_FOCAL_LENGTH_35MM         CONSTRUCT_TAGID(FOCAL_LENGTH_35MM, _ID_FOCAL_LENGTH_35MM)
#define EXIFTAGTYPE_FOCAL_LENGTH_35MM       EXIF_SHORT
// Scene capture type
// Use EXIFTAGTYPE_SCENE_CAPTURE_TYPE as the exif_tag_type (EXIF_SHORT)
// Count should be 1
#define _ID_SCENE_CAPTURE_TYPE              0xa406
#define EXIFTAGID_SCENE_CAPTURE_TYPE        CONSTRUCT_TAGID(SCENE_CAPTURE_TYPE, _ID_SCENE_CAPTURE_TYPE)
#define EXIFTAGTYPE_SCENE_CAPTURE_TYPE      EXIF_SHORT
// Gain control
// Use EXIFTAGTYPE_GAIN_CONTROL as the exif_tag_type (EXIF_SHORT)
// Count should be 1
#define _ID_GAIN_CONTROL                    0xa407
#define EXIFTAGID_GAIN_CONTROL              CONSTRUCT_TAGID(GAIN_CONTROL, _ID_GAIN_CONTROL)
#define EXIFTAGTYPE_GAIN_CONTROL            EXIF_SHORT
// Contrast
// Use EXIFTAGTYPE_CONTRAST as the exif_tag_type (EXIF_SHORT)
// Count should be 1
#define _ID_CONTRAST                        0xa408
#define EXIFTAGID_CONTRAST                  CONSTRUCT_TAGID(CONTRAST, _ID_CONTRAST)
#define EXIFTAGTYPE_CONTRAST                EXIF_SHORT
// Saturation
// Use EXIFTAGTYPE_SATURATION as the exif_tag_type (EXIF_SHORT)
// Count should be 1
#define _ID_SATURATION                      0xa409
#define EXIFTAGID_SATURATION                CONSTRUCT_TAGID(SATURATION, _ID_SATURATION)
#define EXIFTAGTYPE_SATURATION              EXIF_SHORT
// Sharpness
// Use EXIFTAGTYPE_SHARPNESS as the exif_tag_type (EXIF_SHORT)
// Count should be 1
#define _ID_SHARPNESS                       0xa40a
#define EXIFTAGID_SHARPNESS                 CONSTRUCT_TAGID(SHARPNESS, _ID_SHARPNESS)
#define EXIFTAGTYPE_SHARPNESS               EXIF_SHORT
// Subject distance range
// Use EXIFTAGTYPE_SUBJECT_DISTANCE_RANGE as the exif_tag_type (EXIF_SHORT)
// Count should be 1
#define _ID_SUBJECT_DISTANCE_RANGE          0xa40c
#define EXIFTAGID_SUBJECT_DISTANCE_RANGE    CONSTRUCT_TAGID(SUBJECT_DISTANCE_RANGE, _ID_SUBJECT_DISTANCE_RANGE)
#define EXIFTAGTYPE_SUBJECT_DISTANCE_RANGE  EXIF_SHORT
// PIM tag
// Use EXIFTAGTYPE_PIM_TAG as the exif_tag_type (EXIF_UNDEFINED)
// Count can be any
#define _ID_PIM                             0xc4a5
#define EXIFTAGID_PIM_TAG                   CONSTRUCT_TAGID(PIM, _ID_PIM)
#define EXIFTAGTYPE_PIM_TAG                 EXIF_UNDEFINED
#endif // #ifndef _EXIF_H

