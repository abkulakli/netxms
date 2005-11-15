#if !defined(AFX_OBJECTTOOLSEDITOR_H__F2EFBBA9_DB03_4B91_8456_F7CE2B283155__INCLUDED_)
#define AFX_OBJECTTOOLSEDITOR_H__F2EFBBA9_DB03_4B91_8456_F7CE2B283155__INCLUDED_

#include "..\..\..\INCLUDE\nxclapi.h"	// Added by ClassView
#if _MSC_VER > 1000
#pragma once
#endif // _MSC_VER > 1000
// ObjectToolsEditor.h : header file
//

/////////////////////////////////////////////////////////////////////////////
// CObjectToolsEditor frame

class CObjectToolsEditor : public CMDIChildWnd
{
	DECLARE_DYNCREATE(CObjectToolsEditor)
protected:
	CObjectToolsEditor();           // protected constructor used by dynamic creation

// Attributes
public:

// Operations
public:

// Overrides
	// ClassWizard generated virtual function overrides
	//{{AFX_VIRTUAL(CObjectToolsEditor)
	protected:
	virtual BOOL PreCreateWindow(CREATESTRUCT& cs);
	//}}AFX_VIRTUAL

// Implementation
protected:
	NXC_OBJECT_TOOL *m_pToolList;
	DWORD m_dwNumTools;
	CImageList m_imageList;
	CListCtrl m_wndListCtrl;
	virtual ~CObjectToolsEditor();

	// Generated message map functions
	//{{AFX_MSG(CObjectToolsEditor)
	afx_msg int OnCreate(LPCREATESTRUCT lpCreateStruct);
	afx_msg void OnDestroy();
	afx_msg void OnSetFocus(CWnd* pOldWnd);
	afx_msg void OnSize(UINT nType, int cx, int cy);
	afx_msg void OnClose();
	afx_msg void OnViewRefresh();
	afx_msg void OnUpdateObjecttoolsDelete(CCmdUI* pCmdUI);
	afx_msg void OnUpdateObjecttoolsEdit(CCmdUI* pCmdUI);
	afx_msg void OnObjecttoolsEdit();
	afx_msg void OnContextMenu(CWnd* pWnd, CPoint point);
	//}}AFX_MSG
	DECLARE_MESSAGE_MAP()
};

/////////////////////////////////////////////////////////////////////////////

//{{AFX_INSERT_LOCATION}}
// Microsoft Visual C++ will insert additional declarations immediately before the previous line.

#endif // !defined(AFX_OBJECTTOOLSEDITOR_H__F2EFBBA9_DB03_4B91_8456_F7CE2B283155__INCLUDED_)
