#ifndef __DLLLIBPLIST__
#define __DLLLIBPLIST__

#include <plist/plist.h>

class DllLibPlistInterface
{
public:
	virtual ~DllLibPlistInterface() {}

	virtual void        plist_from_bin        (const char *plist_bin,   uint32_t length, plist_t * plist  )=0;
	virtual plist_t     plist_new_dict        (void                                                       )=0;
	virtual uint32_t    plist_dict_get_size   (plist_t node                                               )=0;
	virtual void        plist_get_string_val  (plist_t node,            char **val                        )=0;
	virtual void        plist_get_real_val    (plist_t node,            double *val                       )=0;
	virtual plist_t     plist_dict_get_item   (plist_t node,            const char* key                   )=0;
	virtual void        plist_free            (plist_t plist                                              )=0;
#ifdef TARGET_WINDOWS
	virtual void        plist_free_string_val (char *val                                                  )=0;
#endif
};

class DllLibPlist : public DllLibPlistInterface
{
public:
	DllLibPlist();
	~DllLibPlist();
	bool Load(void);
	void Unload(void);
	bool IsLoaded(void);
	bool EnableDelayedUnload(bool onOff);

	plist_t plist_new_dict(void);
	plist_t plist_dict_get_item(plist_t node, const char* key);
	uint32_t plist_dict_get_size(plist_t node);
	void plist_from_bin(const char *plist_bin, uint32_t length, plist_t * plist);
	void plist_get_string_val(plist_t node, char **val);
	void plist_get_real_val(plist_t node, double *val);
	void plist_free(plist_t plist);
#ifdef TARGET_WINDOWS
	void plist_free_string_val (char *val);
#endif
private:
	plist_t (*_plist_new_dict)(void);
	plist_t (*_plist_dict_get_item)(plist_t node, const char* key);
	uint32_t (*_plist_dict_get_size)(plist_t node);
	void (*_plist_from_bin)(const char *plist_bin, uint32_t length, plist_t * plist);
	void (*_plist_get_string_val)(plist_t node, char **val);
	void (*_plist_get_real_val)(plist_t node, double *val);
	void (*_plist_free)(plist_t plist);
#ifdef TARGET_WINDOWS
	void (*_plist_free_string_val)(char *val);
#endif
	void *m_handler;


};
#endif
